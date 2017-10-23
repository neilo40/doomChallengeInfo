import java.io.{File, FileInputStream}
import java.nio.{ByteBuffer, ByteOrder, MappedByteBuffer}
import java.nio.channels.FileChannel.MapMode._

case class Wad(wadType: String, numLumps: Int, levels: List[Level]) {
  override def toString: String = "[Wad] type: " + wadType + ", lumps: " + numLumps + ", levels: " + levels
}

case class Level(name: String, lumps: Map[String, Lump], lines: Option[List[WadLine]] = None,
                 var quadTree: Option[LineMXQuadTree] = None, var playerStart: Option[Vertex] = None,
                var sectors: Option[List[Sector]] = None){
  def addLump(lump: Lump): Level = {
    val newLumps: Map[String, Lump] = lumps + (lump.name -> lump)
    Level(this.name, newLumps, this.lines, this.quadTree, this.playerStart, this.sectors)
  }

  def setLines(lines: List[WadLine]): Level =
    Level(this.name, this.lumps, Some(lines), this.quadTree, this.playerStart, this.sectors)

  def setSectors(sectors: List[Sector]): Level =
    Level(this.name, this.lumps, this.lines, this.quadTree, this.playerStart, Some(sectors))

  def setPlayerStart(v: Vertex): Unit = this.playerStart = Some(v)

  override def toString: String = "[Level] name: " + name
}

case class Lump(name: String, data: List[Byte]) {
  override def toString: String = "[Lump] name: " + name
}

case class WadLine(a: Vertex, b: Vertex, oneSided: Boolean, sectorTag: Option[Int] = None) {
  override def toString: String = s"[Line] $a <-> $b, oneSided: $oneSided"

  def intersectsWith(that: WadLine): Boolean = {
    val denom: Double = (that.b.y - that.a.y) * (this.b.x - this.a.x) - (that.b.x - that.a.x) * (this.b.y - this.a.y)
    if (denom == 0.0) return false
    val ua: Double = ((that.b.x - that.a.x) * (this.a.y - that.a.y) - (that.b.y - that.a.y) * (this.a.x - that.a.x)) / denom
    val ub: Double = ((this.b.x - this.a.x) * (this.a.y - that.a.y) - (this.b.y - this.a.y) * (this.a.x - that.a.x)) / denom
    if (ua >= 0.0 && ua <= 1.0 && ub >= 0.0 && ub <= 1.0) return true
    false
  }
}

case class Vertex(x: Int, y: Int) {
  override def toString: String = s"($x, $y)"

  def +(that: Vertex): Vertex = {
    Vertex(x + that.x, y + that.y)
  }
}

case class Thing(position: Vertex, facing: Int, doomId: Int)

case class Sector(sectorType: Int, tag: Int)


object WadParser {
  val HEADER_SIZE = 12

  private def createStream(fromFile: String): MappedByteBuffer = {
    val file = new File(fromFile)
    val fileSize = file.length
    val stream = new FileInputStream(file)
    val buffer = stream.getChannel.map(READ_ONLY, 0, fileSize)
    buffer.order(ByteOrder.LITTLE_ENDIAN)
    buffer
  }

  private def extractWadType(byteStream: MappedByteBuffer): String = {
    val wadTypeBytes = new Array[Byte](4)
    byteStream.get(wadTypeBytes, 0, 4)
    wadTypeBytes.map(_.toChar).mkString
  }

  private def extractNumLumps(byteStream: MappedByteBuffer): Int = byteStream.getInt()

  private def extractData(byteStream: MappedByteBuffer): ByteBuffer = {
    val dataEnd = byteStream.getInt()
    val dataBytes = byteStream.slice()
    byteStream.position(dataEnd)
    dataBytes
  }

  private def extractLump(byteStream: MappedByteBuffer, data: ByteBuffer): Lump = {
    val filePos = byteStream.getInt() - HEADER_SIZE
    val size = byteStream.getInt()
    val nameBytes = new Array[Byte](8)
    byteStream.get(nameBytes, 0, 8)
    val name = nameBytes.map(_.toChar).mkString.trim()
    val dataBytes = new Array[Byte](size)
    if (filePos >= 0) {
      data.position(filePos)
      data.get(dataBytes, 0, size)
      Lump(name, dataBytes.toList)
    } else {
      Lump(name, List())
    }
  }

  private def extractLumps(byteStream: MappedByteBuffer, data: ByteBuffer): List[Lump] =
    byteStream.remaining() match {
      case 0 => List()
      case _ => extractLump(byteStream, data) +: extractLumps(byteStream, data)
    }

  private def levelNameMatched(currentLevel: Option[Level], levelName: String, name: String,
                               remainingLumps: List[Lump]): List[Level] = {
    if (currentLevel.isDefined){
      List(currentLevel.get) ++ extractLevels(Some(Level(levelName, Map())), remainingLumps.tail, levelName)
    } else {
      extractLevels(Some(Level(levelName, Map())), remainingLumps.tail, levelName)
    }
  }

  private def nonLevelNameMatched(currentLevel: Option[Level], name: String, currentLump: Lump,
                                  remainingLumps: List[Lump]): List[Level] = {
    if (name == "START"){
      extractLevels(None, remainingLumps.tail, "START")
    } else {
      extractLevels(Some(currentLevel.get.addLump(currentLump)), remainingLumps.tail, name)
    }
  }

  private def extractLevels(currentLevel: Option[Level], remainingLumps: List[Lump], name: String): List[Level] = {
    val currentLump = remainingLumps.headOption.getOrElse(Lump("FINISH", List()))
    val pattern = "(E[0-9]M[0-9])".r
    currentLump.name match {
      case pattern(levelName) => levelNameMatched(currentLevel, levelName, name, remainingLumps)
      case "FINISH" => List(currentLevel.get)
      case _ => nonLevelNameMatched(currentLevel, name, currentLump, remainingLumps)
    }
  }

  private def extractVertex(bytes: List[Byte]): Vertex = {
    val x = ByteBuffer.wrap(bytes.take(2).toArray).order(ByteOrder.LITTLE_ENDIAN).getShort().toInt
    val y = ByteBuffer.wrap(bytes.slice(2, 4).toArray).order(ByteOrder.LITTLE_ENDIAN).getShort().toInt
    Vertex(x, y)
  }

  private def extractVerticesForLevel(level: Level): List[Vertex] = {
    val vertexData = level.lumps("VERTEXES").data
    vertexData.sliding(4, 4).map(extractVertex).toList
  }

  private def extractLine(bytes: List[Byte], vertices: List[Vertex]): WadLine = {
    val aIndex = ByteBuffer.wrap(bytes.take(2).toArray).order(ByteOrder.LITTLE_ENDIAN).getShort().toInt
    val bIndex = ByteBuffer.wrap(bytes.slice(2, 4).toArray).order(ByteOrder.LITTLE_ENDIAN).getShort().toInt
    val flags = ByteBuffer.wrap(bytes.slice(4, 6).toArray).order(ByteOrder.LITTLE_ENDIAN).getShort().toInt
    val specialType = ByteBuffer.wrap(bytes.slice(6, 8).toArray).order(ByteOrder.LITTLE_ENDIAN).getShort().toInt
    val sectorTag = ByteBuffer.wrap(bytes.slice(8, 10).toArray).order(ByteOrder.LITTLE_ENDIAN).getShort().toInt
    val leftSide = ByteBuffer.wrap(bytes.slice(10, 12).toArray).order(ByteOrder.LITTLE_ENDIAN).getShort().toInt
    val rightSide = ByteBuffer.wrap(bytes.slice(12, 14).toArray).order(ByteOrder.LITTLE_ENDIAN).getShort().toInt
    val oneSided = leftSide == -1 || rightSide == -1 || (flags & 0x0001) == 1
    WadLine(vertices(aIndex), vertices(bIndex), oneSided, Some(sectorTag))
  }

  private def extractLinesForLevel(level: Level): List[WadLine] = {
    val vertices = extractVerticesForLevel(level)
    val linedefs = level.lumps("LINEDEFS").data
    linedefs.sliding(14, 14).map(extractLine(_, vertices)).toList
  }

  private def extractSector(bytes: List[Byte]): Sector = {
    val sectorType = ByteBuffer.wrap(bytes.slice(22, 24).toArray).order(ByteOrder.LITTLE_ENDIAN).getShort().toInt
    val tag = ByteBuffer.wrap(bytes.slice(24, 26).toArray).order(ByteOrder.LITTLE_ENDIAN).getShort().toInt
    Sector(sectorType, tag)
  }

  private def extractSectorsForLevel(level: Level): List[Sector] = {
    val sectorBytes = level.lumps("SECTORS").data
    (sectorBytes.sliding(26, 26) map extractSector).toList
  }

  private def addMiscDataToLevel(level: Level): Level = {
    val levelWithLines = level.setLines(extractLinesForLevel(level))
    levelWithLines.setPlayerStart(extractPlayerStart(level))
    val levelWithSectors = levelWithLines.setSectors(extractSectorsForLevel(level))
    levelWithSectors
  }

  private def extractThing(bytes: List[Byte]): Thing = {
    val position = extractVertex(bytes.take(4))
    val angle = ByteBuffer.wrap(bytes.slice(4, 6).toArray).order(ByteOrder.LITTLE_ENDIAN).getShort().toInt
    val doomId = ByteBuffer.wrap(bytes.slice(6, 8).toArray).order(ByteOrder.LITTLE_ENDIAN).getShort().toInt
    Thing(position, angle, doomId)
  }

  private def extractPlayerStart(level: Level): Vertex = {
    val thingsLump = level.lumps("THINGS").data
    val things = (thingsLump.sliding(10, 10) map extractThing).toList
    things.find(_.doomId == 1).get.position
  }

  def createWad(fromFile: String = "/home/neil/Downloads/doom1.wad"): Wad = {
    val byteStream = createStream(fromFile)
    val wadType = extractWadType(byteStream)
    val numLumps = extractNumLumps(byteStream)
    val data = extractData(byteStream)
    val lumps = extractLumps(byteStream, data)
    val levels = extractLevels(None, lumps, "START").map(addMiscDataToLevel)
    levels foreach {level => level.quadTree = Some(LineMXQuadTree.createQuadTree(level))}

    Wad(wadType, numLumps, levels)
  }
}
