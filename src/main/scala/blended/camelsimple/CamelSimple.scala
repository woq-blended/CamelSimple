package blended.camelsimple

import java.util.Date

import akka.actor.ActorSystem
import blended.jms.utils.{BlendedJMSConnectionConfig, BlendedSingleConnectionFactory, ConnectionException}
import com.pcbsys.nirvana.nJMS.ConnectionFactoryImpl
import com.pcbsys.nirvana.nSpace.NirvanaContextFactory
import com.typesafe.config.ConfigFactory
import javax.jms.{ConnectionFactory, ExceptionListener, JMSException}
import org.apache.camel.builder._
import org.apache.camel.component.jms.JmsComponent
import org.apache.camel.impl.{DefaultCamelContext, SimpleRegistry}
import org.apache.camel.{Exchange, LoggingLevel, Processor}
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

      //applyACL(ChannelACLConfig(channel = "/Sample.*", user = "sib", purge=true))
      //applyACL(ChannelACLConfig(channel = "/global.*", user = "sib", purge=true))
    }
    connector.close()
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

  private val exceptionHandler : Exception => Unit = { e =>

    def getJmsCause(current : Throwable) : Option[JMSException] = current match {
      case jmse : JMSException => Some(jmse)
      case o => Option(o) match {
        case None => None
        case Some(same) if same == same.getCause() => None
        case Some(e) => getJmsCause(e.getCause())
      }
    }

    getJmsCause(e) match {
      case None =>
      case Some(illegalState) if illegalState.isInstanceOf[javax.jms.IllegalStateException] =>
        system.eventStream.publish(ConnectionException("sagum", "sagum", illegalState))
      case jmse =>
    }

  }

  private val exceptionListener = new ExceptionListener {
    override def onException(exception: JMSException): Unit = exceptionHandler(exception)
  }

  private val camelContext = {
    val ctxt = new DefaultCamelContext(new SimpleRegistry())

    ctxt.getRegistry(classOf[SimpleRegistry]).put("el", exceptionListener)
    ctxt.addComponent("sagum", JmsComponent.jmsComponent(blendedCf))

    ctxt
  }

  private val routes = new RouteBuilder {
    override def configure(): Unit = {

      onException(classOf[Exception])
        .process(new Processor {
          override def process(exchange: Exchange): Unit = {

            Option(exchange.getProperties().get(Exchange.EXCEPTION_CAUGHT)) match {
              case None =>
              case Some(e) =>
                log.warn(s"Caught exception of type : [${e.getClass().getName()}]")
                exceptionHandler(e.asInstanceOf[Exception])
              }
            }
          }
      ).handled(true)

      from("scheduler://foo?delay=1000")
        .process(new Processor {
          override def process(exchange: Exchange): Unit = {
            exchange.getOut().setBody(new Date().toString())
          }
        })
        .to("sagum:/SampleQueue?deliveryMode=2")
        .log(LoggingLevel.INFO, "Sent: ${body}")

      from("sagum:/SampleQueue?consumerType=Default&acknowledgementModeName=CLIENT_ACKNOWLEDGE&cacheLevelName=CACHE_NONE&exceptionListener=#el")
        .log(LoggingLevel.INFO, "Received: ${body}")

      setErrorHandlerBuilder(new LoggingErrorHandlerBuilder())
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
