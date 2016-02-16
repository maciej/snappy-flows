package me.maciejb.snappyflows.util

import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.stream.stage.{Context, PushPullStage, SyncDirective, TerminationDirective}
import akka.util.ByteString


private[snappyflows] object Chunking {

  def fixedSize(chunkSize: Int): Flow[ByteString, ByteString, NotUsed] =
    Flow[ByteString].transform(() => new ChunkerStage(chunkSize))

  class ChunkerStage(val chunkSize: Int) extends PushPullStage[ByteString, ByteString] {

    var buffer = ByteString.empty

    override def onPush(chunk: ByteString, ctx: Context[ByteString]) = {
      buffer ++= chunk
      split(ctx)
    }

    override def onPull(ctx: Context[ByteString]) = {
      split(ctx)
    }

    private def tryPull(ctx: Context[ByteString]): SyncDirective = {
      if (ctx.isFinishing) ctx.pushAndFinish(buffer)
      else ctx.pull()
    }

    def split(ctx: Context[ByteString]): SyncDirective = {
      if (buffer.size < chunkSize) tryPull(ctx)
      else {
        val (chunk, remaining) = buffer.splitAt(chunkSize)
        buffer = remaining
        if (ctx.isFinishing && buffer.isEmpty) ctx.pushAndFinish(chunk)
        else ctx.push(chunk)
      }
    }

    override def onUpstreamFinish(ctx: Context[ByteString]): TerminationDirective = {
      if (buffer.nonEmpty) ctx.absorbTermination()
      else ctx.finish()
    }

    override def postStop() = buffer = null
  }
}
