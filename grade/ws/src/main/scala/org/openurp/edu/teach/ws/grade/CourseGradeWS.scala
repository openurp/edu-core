package org.openurp.edu.teach.ws.grade

import org.beangle.data.jpa.dao.OqlBuilder
import org.beangle.webmvc.api.annotation.{ mapping, response }
import org.beangle.webmvc.entity.action.{ AbstractEntityAction, RestfulService }
import org.openurp.edu.teach.grade.CourseGrade
import org.beangle.webmvc.api.context.Params
import org.beangle.webmvc.api.annotation.param
import org.beangle.data.model.Entity
import org.openurp.edu.teach.ds.lesson.OutputProperties


class CourseGradeWS extends AbstractEntityAction[CourseGrade] {

  @response
  @mapping(value = "{stdId}")
  def std(@param("stdId") stdId: String): Seq[CourseGrade] = {
    val builder = OqlBuilder.from(classOf[CourseGrade], "cg")
    builder.where("cg.std.id=:stdId", stdId.toLong)
    builder.where("cg.project.code = :project", Params.get("project").get)
    entityDao.search(builder)
 }
  
  @response
  @mapping(value = "{code}")
  def index(@param("code") code: String): Seq[CourseGrade] = {
    val builder = OqlBuilder.from(classOf[CourseGrade], "cg")
    builder.where("cg.std.code=:code", code)
    builder.where("cg.project.code = :project", Params.get("project").get)
    entityDao.search(builder)
  }
}

