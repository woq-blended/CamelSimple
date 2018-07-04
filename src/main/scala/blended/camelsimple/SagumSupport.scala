package blended.camelsimple

import blended.jms.utils.BlendedJMSConnectionConfig
import com.pcbsys.nirvana.nJMS.ConnectionFactoryImpl
import com.pcbsys.nirvana.nSpace.NirvanaContextFactory
import com.typesafe.config.Config

import scala.util.Try

trait SagumSupport { this : SagumMgmtTasks =>

  private[this] var connector : Option[SagDomainConnector] = None

  def sagConnector(config: Config): SagDomainConnector = connector match {
    case None =>
      val result = SagDomainConnector(config)
      connector = Some(result)
      result
    case Some(c) => c
  }

  def sagCf(brokerUrl : String) = {
    val cf = new ConnectionFactoryImpl(brokerUrl)
    cf.setEnableSharedDurable(true)
    cf.setMaxReconAttempts(0)
    cf.setConxExceptionOnFailure(true)
    cf.setAutoCreateResource(false)
    cf.setImmediateReconnect(false)
    cf.setFollowMaster(true)
    cf
  }

  def jmsConnectionConfig(config: Config): BlendedJMSConnectionConfig =
    BlendedJMSConnectionConfig.fromConfig(s => Try(s))(
      "sagum", "sagum", config
    ).copy(
      cfEnabled = { Option(_ => true) },
      cfClassName = Option(classOf[ConnectionFactoryImpl].getName()),
      ctxtClassName = Option(classOf[NirvanaContextFactory].getName())
    )

}
