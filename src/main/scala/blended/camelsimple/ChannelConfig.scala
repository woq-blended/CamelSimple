package blended.camelsimple

sealed abstract class ChannelType(t : Integer) {
  val cType : Integer = t
}

object Reliable extends ChannelType(1)
object Persistent extends ChannelType(2)
object Mixed extends ChannelType(3)
object Simple extends ChannelType(4)
object Transient extends ChannelType(5)
object OffHeap extends ChannelType(7)

case class ChannelConfig(
  name : String,
  isQueue : Boolean = true,
  capacity : Integer  = 0,
  ttl : Long  = 0,
  channelType : ChannelType = Reliable,
  clustered : Boolean = true,
  autoMaintain : Boolean = true,
  honourCapacity : Boolean = false,
  enableCaching : Boolean = true,
  cacheOnRelod : Boolean = true,
  enableReadBuffering : Boolean = true,
  readBufferSize : Long = 10240,
  syncEachWrite : Boolean = false,
  priority : Integer = 4
) {

  def fqName : String = {
    var result = name
    if (!result.startsWith("/")) result = s"/$result"
    if (result.endsWith("/")) result = result.take(result.length - 1)

    result
  }

  def withIsQueue(isQueue: Boolean) = copy(isQueue = isQueue)
  def withCapacity(capacity: Integer) = copy(capacity = capacity)
  def withTtl(ttl : Long) = copy(ttl = ttl)
  def withChannelType(cType : ChannelType) = copy(channelType = channelType)
  def withClustered(clustered : Boolean) = copy(clustered = clustered)
  def withAutoMaintain(autoMaintain : Boolean) = copy(autoMaintain = autoMaintain)
  def withHonourCapacity(honourCapacity : Boolean) = copy(honourCapacity = honourCapacity)
  def withEnableCaching(enableCaching : Boolean) = copy(enableCaching = enableCaching)
  def withCacheOnRelod(cacheOnReload : Boolean) = copy(cacheOnRelod = cacheOnRelod)
  def withEnableReadBuffering(enableReadBuffering: Boolean) = copy(enableReadBuffering = enableReadBuffering)
  def withReadBufferSize(readBufferSize : Long) = copy(readBufferSize = readBufferSize)
  def withSyncEachWrite(syncEachWrite : Boolean) = copy(syncEachWrite = syncEachWrite)
  def withPriority(priority : Int) = copy(priority = priority)
}


