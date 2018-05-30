package blended.camelsimple

import java.util
import javax.naming.{Context, InitialContext}

import com.pcbsys.nirvana.client.{nSession, nSessionAttributes, nSessionFactory}
import com.pcbsys.nirvana.nAdminAPI.nRealmNode
import com.pcbsys.nirvana.nSpace.NirvanaContextFactory
import com.typesafe.config.Config

object SagDomainConnector {

  private[this] def getStringOption(cfg: Config, path : String) : Option[String] = {
    if (cfg.hasPath(path))
      Some(cfg.getString(path))
    else
      None
  }

  def apply(cfg: Config) = new SagDomainConnector(
    cfg.getString("url"),
    cfg.getString("user"),
    getStringOption(cfg, "password")
  )
}

case class SagDomainConnector(url : String, user: String, password: Option[String]) {

  private[this] var jndiContext : Option[Context] = None
  private[this] var msgSession : Option[nSession] = None
  private[this] var rNode : Option[nRealmNode] = None

  def initialContext() : Context = jndiContext match {
    case Some(ctxt) => ctxt
    case None =>
      val env : util.Hashtable[String,String] = new util.Hashtable[String, String]()

      env.put(Context.PROVIDER_URL, url)
      env.put(Context.INITIAL_CONTEXT_FACTORY, classOf[NirvanaContextFactory].getName())
      env.put(Context.SECURITY_PRINCIPAL, user)
      password.foreach(env.put(Context.SECURITY_CREDENTIALS, _))

      val ctxt = new InitialContext(env)
      jndiContext = Some(ctxt)

      ctxt
  }

  def messagingSession : nSession = msgSession match {
    case Some(session) => session
    case None =>
      val nsa: nSessionAttributes = new nSessionAttributes(Array(url))

      val session = password match {
        case Some(pwd) => nSessionFactory.create(nsa, user, pwd)
        case None => nSessionFactory.create(nsa, user)
      }

      session.init()
      msgSession = Some(session)
      session
  }

  def realmNode : nRealmNode = rNode match {
    case Some(n) => n
    case None =>
      val nsa : nSessionAttributes = new nSessionAttributes(url)
      val n = password match {
        case Some(pwd) => new nRealmNode(nsa, user, pwd)
        case None => new nRealmNode(nsa, user)
      }

      n.waitForEntireNameSpace()
      rNode = Some(n)
      n
  }

  def close() : Unit = {
    jndiContext.foreach(_.close())
    msgSession.foreach(_.close())
    rNode.foreach(_.close())
  }

}
