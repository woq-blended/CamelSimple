package blended.camelsimple

case class ChannelACLConfig(
  channel : String,
  user : String = "Everyone",
  host : Option[String] = None,
  purge : Boolean = false,
  pop : Boolean = true,
  read : Boolean = true,
  write : Boolean = true
)
