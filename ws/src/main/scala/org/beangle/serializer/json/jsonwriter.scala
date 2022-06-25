/*
 * Copyright (C) 2014, The OpenURP Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.beangle.serializer.json

import java.io.Writer

import org.beangle.serializer.text.marshal.MarshallerRegistry
import org.beangle.serializer.text.marshal.Type.{Collection, Object}

class DefaultJsonWriter(writer: Writer, registry: MarshallerRegistry) extends AbstractJsonWriter(writer, registry) {

  override def startNode(name: String, clazz: Class[_]): Unit = {
    val depth = pathStack.size
    val inArray = depth > 0 && registry.lookup(this.pathStack.peek().clazz).targetType == Collection
    pathStack.push(name, clazz)
    if (!pathStack.isFirstInLevel) {
      writer.write(',')
    }
    if (!inArray && depth > 0) {
      writer.write("\"")
      writer.write(name)
      writer.write("\":")
    }
    registry.lookup(clazz).targetType match {
      case Collection => writer.write('[')
      case Object => writer.write('{')
      case _ =>
    }
  }

  override def addAttribute(key: String, value: String): Unit = {
    writer.write(" \"")
    writer.write(key)
    writer.write("\":")
    writeText(value.toCharArray, quoted = true)
  }

  override def endNode(): Unit = {
    val clazz = pathStack.pop().clazz
    registry.lookup(clazz).targetType match {
      case Collection => writer.write(']')
      case Object => writer.write('}')
      case _ =>
    }
    if (pathStack.size == 0) writer.flush()
  }

}
