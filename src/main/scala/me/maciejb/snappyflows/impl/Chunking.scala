package me.maciejb.snappyflows.impl

import akka.NotUsed
import akka.stream.{Outlet, Inlet, Attributes, FlowShape}
import akka.stream.scaladsl.Flow
import akka.stream.stage._
import akka.util.ByteString


private[snappyflows] object Chunking {

  def fixedSize(chunkSize: Int): Flow[ByteString, ByteString, NotUsed] =
    Flow[ByteString].via(new ChunkerStage(chunkSize))

  class ChunkerStage(chunkSize: Int) extends GraphStage[FlowShape[ByteString, ByteString]] {
    require(chunkSize > 0, "`chunkSize` should be greater than 0")

    val in = Inlet[ByteString]("bytes-in")
    val out = Outlet[ByteString]("bytes-out")

    override val shape = FlowShape(in, out)
    override def createLogic(inheritedAttributes: Attributes) = {

      var buffer = ByteString.empty

      new GraphStageLogic(shape) {
        private def nextChunk(): ByteString = {
          val (chunk, remaining) = buffer.splitAt(chunkSize)
          buffer = remaining
          chunk
        }

        private def tryPull(): Unit = if (!isClosed(in)) pull(in)

        private def split() = {
          if (buffer.size < chunkSize) tryPull()
          else push(out, nextChunk())
        }

        setHandler(in, new InHandler {
          @throws[Exception]
          override def onPush() = {
            buffer ++= grab(in)
            split()
          }

          @throws[Exception]
          override def onUpstreamFinish() = {
            while (buffer.nonEmpty) emit(out, nextChunk())
            complete(out)
          }
        })

        setHandler(out, new OutHandler {
          @throws[Exception]
          override def onPull() = {
            split()
          }
        })

      }
    }
  }

}
