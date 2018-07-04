package blended.jms.utils.internal

import blended.camelsimple.{ChannelConfig, SagDomainConnector, SagumMgmtTasks, SagumSupport}
import blended.jms.utils.BlendedSingleConnectionFactory
import javax.jms.Connection
import org.scalatest.BeforeAndAfterAll
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal
import scala.concurrent.duration._

class SagumPingPerformerSpec extends JMSPingPerformerSpec
  with SagumSupport
  with SagumMgmtTasks
  with BeforeAndAfterAll {

  override val log: Logger = LoggerFactory.getLogger(classOf[SagumPingPerformerSpec])
  override var con: Option[Connection] = None

  override val pingQueue: String = "global/sib/ping"
  override val pingTopic: String = "blended/ping"

  private[this] val config = system.settings.config
  override val cfg = jmsConnectionConfig(config.getConfig("sagumCF"))

  override val bulkCount: Int = 10000

  override val bulkTimeout: FiniteDuration = 1.hour

  override protected def beforeAll(): Unit = {

    if (config.getBoolean("setup")) {

      implicit val connector : SagDomainConnector = SagDomainConnector(config.getConfig("sagConnector"))

      val jndiName = config.getString("jndiName")

      val cf = sagCf(config.getString("brokerUrl"))

      bindObject(cf, jndiName)(connector)

      createChannel(ChannelConfig(name = pingTopic, isQueue = false))
      createChannel(ChannelConfig(name = pingQueue, isQueue = true))

      connector.close()
    }

    val blendedCF = new BlendedSingleConnectionFactory(
      cfg,
      system,
      None
    )

    var connected = false

    while(!connected) {
      try {
        con = Some(blendedCF.createConnection())
        connected = true
      } catch {
        case NonFatal(e) => Thread.sleep(1000)
      }
    }
  }

}
