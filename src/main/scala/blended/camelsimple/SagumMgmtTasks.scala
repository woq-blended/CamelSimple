package blended.camelsimple

import com.pcbsys.nirvana.client.{nChannelAttributes, nFindResult}
import com.pcbsys.nirvana.nAdminAPI.{nChannelACLEntry, nContainer, nLeafNode, nNode}
import com.pcbsys.nirvana.nJMS.{QueueImpl, TopicImpl}
import javax.jms.Destination
import javax.naming.{NameNotFoundException, Referenceable}
import org.slf4j.Logger

import scala.collection.convert.Wrappers.JEnumerationWrapper
import scala.util.Try

trait SagumMgmtTasks {

  val log : Logger

  def findChannel(name: String)(implicit connector: SagDomainConnector) : Option[nChannelAttributes] = {
    val session = connector.messagingSession

    val search = new nChannelAttributes()
    search.setName(name)
    val q : nFindResult = session.find(Array(search))(0)

    if (Option(q.getException()).isEmpty) {
      if (q.isChannel)
        Some(q.getChannel().getChannelAttributes())
      else
        Some(q.getQueue().getQueueAttributes())
    } else {
      None
    }
 }

  def createChannel(qCfg : ChannelConfig)(implicit connector: SagDomainConnector) : Try[Destination] = {

    val session = connector.messagingSession

    val cattrib : nChannelAttributes = findChannel(qCfg.fqName) match {
      case Some(a) =>
        log.info("Reusing channel definition [{}]", qCfg.fqName)
        a
      case None =>
        log.info("Creating channel definition [{}]", qCfg.fqName)
        val a = new nChannelAttributes()
        a.setName(qCfg.fqName)
        if (qCfg.isQueue) {
          a.setChannelMode(nChannelAttributes.QUEUE_MODE)
          session.createQueue(a)
        }
        else {
          a.setChannelMode(nChannelAttributes.CHANNEL_MODE)
          session.createChannel(a)
        }
        a
    }

    cattrib.setMaxEvents(qCfg.capacity)
    cattrib.setTTL(qCfg.ttl)
    cattrib.setType(qCfg.channelType.cType)
    cattrib.setClusterWide(session.isMemberOfCluster() && qCfg.clustered)

    cattrib.getProperties().setPerformAutomaticMaintenance(qCfg.autoMaintain)
    cattrib.getProperties().setHonorCapacityWhenFull(qCfg.honourCapacity)
    cattrib.getProperties().setEnableCaching(qCfg.enableCaching)
    cattrib.getProperties().setCacheOnReload(qCfg.cacheOnRelod)
    cattrib.getProperties().setEnableReadBuffering(qCfg.enableReadBuffering)
    cattrib.getProperties().setReadBufferSize(qCfg.readBufferSize)
    cattrib.getProperties().setSyncOnEachWrite(qCfg.syncEachWrite)
    cattrib.getProperties().setPriority(qCfg.priority)

    cattrib.useJMSEngine(true)

    Try {
      qCfg.isQueue match {
        case true =>
          new QueueImpl(qCfg.name)
        case false =>
          new TopicImpl(qCfg.name)
      }
    }
  }

  def bindObject(obj: Referenceable, jndiName: String)(implicit connector: SagDomainConnector) : Try[String] = {
    log.info(s"Binding object of type [${obj.getClass().getName()}] to JNDI name [$jndiName]")

    Try {
      try {
        connector.initialContext().rebind(jndiName, obj)
        jndiName
      } catch {
        case _ : NameNotFoundException =>
          connector.initialContext().bind(jndiName, obj)
          jndiName
      }
    }
  }

  def destinationNodes(implicit connector: SagDomainConnector) : List[nLeafNode] = {

    def listNodes(node: nNode) : List[nLeafNode] =
      if (node.isInstanceOf[nLeafNode]) {
        val leaf = node.asInstanceOf[nLeafNode]
        if (leaf.isChannel() || leaf.isQueue()) {
          List(leaf)
        } else
          List.empty
      } else if (node.isInstanceOf[nContainer]) {
        val nodes = JEnumerationWrapper(node.asInstanceOf[nContainer].getNodes()).toList
        nodes.flatMap(n => listNodes(n.asInstanceOf[nNode]))
      } else {
        List.empty
      }

    listNodes(connector.realmNode)
  }

  def applyACL(acl : ChannelACLConfig*)(implicit connector : SagDomainConnector) : Unit = {
    destinationNodes.foreach { dest =>
      acl.foreach { acl =>
        if (dest.getAbsolutePath().matches(acl.channel)) {
          val host = acl.host match {
            case Some(h) => h
            case None => "*"
          }
          val entry = new nChannelACLEntry(acl.user, host)

          entry.setPop(acl.pop)
          entry.setPurge(acl.purge)
          entry.setRead(acl.read)
          entry.setWrite(acl.write)

          log.info(s"Applying [$acl] to [${dest.getAbsolutePath()}]")

          dest.addACLEntry(entry)
        }
      }
    }
  }
}
