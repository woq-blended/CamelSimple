package blended.camelsimple

import java.util.Date

import akka.actor.ActorSystem
import blended.jms.utils.{BlendedJMSConnectionConfig, BlendedSingleConnectionFactory}
import com.pcbsys.nirvana.nJMS.ConnectionFactoryImpl
import com.pcbsys.nirvana.nSpace.NirvanaContextFactory
import com.typesafe.config.ConfigFactory
import javax.jms.ConnectionFactory
import org.apache.camel.{Exchange, LoggingLevel, Processor}
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.jms.JmsComponent
import org.apache.camel.impl.DefaultCamelContext
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try

object CamelSimple {

  def main(args: Array[String]) : Unit = {
    new CamelSimple().start()
  }
}

class CamelSimple extends SagumMgmtTasks {

  override val log : Logger  = LoggerFactory.getLogger(classOf[CamelSimple])
  private val config = ConfigFactory.defaultApplication().resolve()
  private val system = ActorSystem("CamelSimple")

  private implicit val connector : SagDomainConnector = SagDomainConnector(config.getConfig("sagConnector"))

  private val jndiName = config.getString("jndiName")

  private val sagumCf = {
    val cf = new ConnectionFactoryImpl(config.getString("brokerUrl"))
    cf.setEnableSharedDurable(true)
    cf.setMaxReconAttempts(0)
    cf.setConxExceptionOnFailure(true)
    cf.setAutoCreateResource(false)
    cf.setImmediateReconnect(false)
    cf.setFollowMaster(true)
    cf
  }

  // This is just a helper method to initialize a naked SAGUM messaging broker
  private def setup() : Unit = {
    if (config.getBoolean("setup")) {
      bindObject(sagumCf, jndiName)

      createChannel(ChannelConfig(name = "SampleQueue", isQueue = true))
      createChannel(ChannelConfig(name = "global/sib/ping", isQueue = true))

      applyACL(ChannelACLConfig(channel = "/Sample.*", user = "sib", purge=true))
      applyACL(ChannelACLConfig(channel = "/global.*", user = "sib", purge=true))
      connector.close()
    }
  }

  private val jmsConnectionConfig =
    BlendedJMSConnectionConfig.fromConfig(s => Try(s))(
      "sagum", "sagum",
      config.getConfig("sagumCF")
    ).copy(
      cfEnabled = { Option(_ => true) },
      cfClassName = Option(classOf[ConnectionFactoryImpl].getName()),
      ctxtClassName = Option(classOf[NirvanaContextFactory].getName())
    )

  // This is a connection factory that is actually used in the SIB application.
  // Only one connection will be created and maintained from this connection factory wrapper.
  // Under the covers we send a ping every so often to make sure the connection is still operational.

  private val blendedCf : ConnectionFactory = new BlendedSingleConnectionFactory(jmsConnectionConfig, system, None)

  private val camelContext = {
    val ctxt = new DefaultCamelContext()
    ctxt.addComponent("sagum", JmsComponent.jmsComponent(blendedCf))

    ctxt
  }

  private val routes = new RouteBuilder {
    override def configure(): Unit = {

      from("scheduler://foo?delay=1000")
        .process(new Processor {
          override def process(exchange: Exchange): Unit = {
            exchange.getOut().setBody(new Date().toString())
          }
        })
        .to("sagum:/SampleQueue?deliveryMode=2")

      from("sagum:/SampleQueue?cacheLevelName=CACHE_NONE")
        .log(LoggingLevel.INFO, "${body}")
    }
  }

  def start() : Unit = {
    // This will prepare SAGUM
    setup()

    // Now we configure the routes and kick off
    camelContext.addRoutes(routes)
    camelContext.start()
  }
}
