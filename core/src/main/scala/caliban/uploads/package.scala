package caliban

import zio.stream.{ Stream, ZStream }
import zio.{ Chunk, Has, Layer, UIO, URIO, ZIO, ZLayer }

package object uploads {
  type Uploads = Has[Multipart]

  object Uploads {
    val empty: Layer[Nothing, Uploads] =
      ZLayer.succeed(new Multipart {
        def stream(name: String): ZStream[Any, Throwable, Byte] = Stream.empty

        def file(name: String): UIO[Option[FileMeta]] = ZIO.none
      })

    def stream(name: String): ZStream[Uploads, Throwable, Byte] =
      ZStream.accessStream(_.get.stream(name))

    def fileMeta(name: String): URIO[Uploads, Option[FileMeta]] =
      ZIO.accessM(_.get.file(name))

    def handler(fileHandle: String => UIO[Option[FileMeta]]): UIO[Uploads] =
      ZIO
        .succeed(new Multipart {
          def stream(name: String): ZStream[Any, Throwable, Byte] =
            for {
              ref   <- ZStream.fromEffectOption(fileHandle(name).some)
              bytes <- ZStream.fromChunk(Chunk.fromArray(ref.bytes))
            } yield bytes

          def file(name: String): UIO[Option[FileMeta]] =
            fileHandle(name)
        })
        .asService

  }
}
