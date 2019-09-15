package lila.ws
package ipc

import chess.format.{ FEN, Uci, UciCharPair }
import chess.opening.FullOpening
import chess.variant.Crazyhouse
import play.api.libs.json._

import lila.ws.util.LilaJsObject.augment

sealed trait ClientIn extends ClientMsg {
  def write: String
}

object ClientIn {

  import Chess.json._

  case object Pong extends ClientIn {
    val write = "0"
  }

  case class LobbyPong(members: Int, rounds: Int) extends ClientIn {
    val write = Json stringify Json.obj(
      "t" -> "n",
      "d" -> members,
      "r" -> rounds
    )
  }

  case class Fen(game: Game.ID, lastUci: Uci, fen: FEN) extends ClientIn {
    def write = clientMsg("fen", Json.obj(
      "id" -> game,
      "lm" -> lastUci,
      "fen" -> fen
    ))
  }

  case class Mlat(millis: Double) extends ClientIn {
    def write = clientMsg("mlat", millis)
  }

  case class NbMembers(value: Int) extends ClientIn {
    def write = clientMsg("member/nb", value)
  }

  case class NbRounds(value: Int) extends ClientIn {
    def write = clientMsg("round/nb", value)
  }

  case class Payload(json: JsonString) extends ClientIn {
    def write = json.value
  }

  case class NonIdle(payload: Payload) extends ClientIn {
    def write = payload.write
  }

  case class Opening(path: Path, opening: FullOpening) extends ClientIn {
    def write = clientMsg("opening", Json.obj(
      "path" -> path,
      "opening" -> opening
    ))
  }

  case object StepFailure extends ClientIn {
    def write = clientMsg("stepFailure")
  }

  case class Node(
      path: Path,
      id: UciCharPair,
      ply: Int,
      move: chess.format.Uci.WithSan,
      fen: chess.format.FEN,
      check: Boolean,
      dests: Map[chess.Pos, List[chess.Pos]],
      opening: Option[chess.opening.FullOpening],
      drops: Option[List[chess.Pos]],
      crazyData: Option[Crazyhouse.Data],
      chapterId: Option[ChapterId]
  ) extends ClientIn {
    def write = clientMsg("node", Json.obj(
      "path" -> path,
      "node" -> Json.obj(
        "ply" -> ply,
        "fen" -> fen,
        "id" -> id,
        "uci" -> move.uci,
        "san" -> move.san,
        "dests" -> dests,
        "children" -> JsArray()
      ).add("opening" -> opening)
        .add("check" -> check)
        .add("drops", drops.map { drops =>
          JsString(drops.map(_.key).mkString)
        })
        .add("crazy", crazyData)
    )
      .add("ch", chapterId))
  }

  case class Dests(
      path: Path,
      dests: String,
      opening: Option[chess.opening.FullOpening],
      chapterId: Option[ChapterId]
  ) extends ClientIn {
    def write = clientMsg("dests", Json.obj(
      "dests" -> dests,
      "path" -> path
    ).add("opening" -> opening)
      .add("ch", chapterId))
  }

  private def clientMsg[A: Writes](t: String, data: A) = Json stringify Json.obj(
    "t" -> t,
    "d" -> data
  )
  private def clientMsg(t: String) = Json stringify Json.obj(
    "t" -> t
  )
}