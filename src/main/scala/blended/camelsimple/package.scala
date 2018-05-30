package blended

import com.typesafe.config.Config

import scala.reflect.runtime.universe._
import scala.language.implicitConversions

package object camelsimple {

  implicit def string2Type(s: String) : ChannelType = {
    s match {
      case "Reliable" => Reliable
      case "Persistent" => Persistent
      case "Mixed" => Mixed
      case "Simple" => Simple
      case "Transient" => Transient
      case "OffHeap" => OffHeap
    }
  }

  implicit def cfg2Channel(cfg: Config) : ChannelConfig = {

    def optionValue[T](cfg: Config, path: String)(implicit typeTag: TypeTag[T]) : Option[T] = {
      if (cfg.hasPath(path)) {
        Some(cfg.getValue(path).unwrapped().asInstanceOf[T])
      } else {
        None
      }
    }

    var result = new ChannelConfig(cfg.getString("name"))

    optionValue[Boolean](cfg, "isQueue").foreach(isQueue => result = result.withIsQueue(isQueue))
    optionValue[Int](cfg, "capacity").foreach(capacity => result = result.withCapacity(capacity))
    optionValue[Long](cfg, "ttl").foreach(ttl => result = result.withTtl(ttl))
    optionValue[Boolean](cfg, "clustered").foreach(clustered => result = result.withClustered(clustered))
    optionValue[Boolean](cfg, "autoMaintain").foreach(autoMaintain => result = result.withAutoMaintain(autoMaintain))
    optionValue[Boolean](cfg, "honourCapacity").foreach(honourCapacity => result = result.withHonourCapacity(honourCapacity))
    optionValue[Boolean](cfg, "enableCaching").foreach(enableCaching => result = result.withEnableCaching(enableCaching))
    optionValue[Boolean](cfg, "cacheOnRelod").foreach(cacheOnRelod => result = result.withCacheOnRelod(cacheOnRelod))
    optionValue[Boolean](cfg, "enableReadBuffering").foreach(enableReadBuffering => result = result.withEnableReadBuffering(enableReadBuffering))
    optionValue[Boolean](cfg, "syncEachWrite").foreach(syncEachWrite => result = result.withSyncEachWrite(syncEachWrite))

    optionValue[String](cfg, "channelType").foreach{ channelType =>
      val ct : ChannelType = channelType
      result = result.withChannelType(ct)
    }

    optionValue[Long](cfg, "readBufferSize").foreach(readBufferSize => result = result.withReadBufferSize(readBufferSize))
    optionValue[Int](cfg, "priority").foreach(priority => result = result.withPriority(priority))

    result


  }
}
