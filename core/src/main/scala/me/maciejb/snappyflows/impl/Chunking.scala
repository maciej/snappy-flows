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
    val in = Inlet[ByteString]("bytes-in")
    val out = Outlet[ByteString]("bytes-out")

    override val shape = FlowShape(in, out)
    override def createLogic(inheritedAttributes: Attributes) = {

      var buffer = ByteString.empty

      new GraphStageLogic(shape) {
        def split() = {
          if (buffer.size < chunkSize) {
            tryPull()
          } else {
            val (chunk, remaining) = buffer.splitAt(chunkSize)
            buffer = remaining
            if (isClosed(in) && buffer.isEmpty) {
              push(out, chunk)
              complete(out)
            }
            else {
              push(out, chunk)
            }
          }
        }

        private def tryPull(): Unit =
          if (isClosed(in)) {
            push(out, buffer)
            complete(out)
          } else {
            pull(in)
          }

        setHandler(in, new InHandler {
          @throws[Exception]
          override def onPush() = {
            buffer ++= grab(in)
            split()
          }
          @throws[Exception](classOf[Exception])
          override def onUpstreamFinish() = {}
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
