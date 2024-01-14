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

package org.openurp.edu.grade.service.impl

import com.google.gson.Gson
import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.Strings
import org.openurp.base.model.Project
import org.openurp.base.service.ProjectConfigService
import org.openurp.code.edu.model.{CourseTakeType, ExamStatus, GradeType}
import org.openurp.code.service.CodeService
import org.openurp.edu.grade.BaseServiceImpl
import org.openurp.edu.grade.service.impl.CourseGradeSettingsImpl.*
import org.openurp.edu.grade.service.{CourseGradeSetting, CourseGradeSettings}

import java.util as ju

class CourseGradeSettingsImpl extends BaseServiceImpl with CourseGradeSettings {

  var codeService: CodeService = _

  var configService: ProjectConfigService = _

  def getSetting(project: Project): CourseGradeSetting = {
    val settingStr = configService.get(project, "edu.grade.setting", "")
    var setting: CourseGradeSetting = null
    if (Strings.isNotBlank(settingStr)) {
      setting = CourseGradeSettingsImpl.parse(settingStr)
    } else {
      setting = new CourseGradeSetting(project)
    }
    setting
  }

}

object CourseGradeSettingsImpl {
  def parse(str: String): CourseGradeSetting = {
    val gson = new Gson()
    val obj = gson.fromJson(str, classOf[ju.Map[String, Any]])
    val setting = new CourseGradeSetting()
    import scala.jdk.CollectionConverters.*
    obj.get("gaElementTypes").asInstanceOf[ju.List[ju.Map[String, Any]]].asScala foreach { eleType =>
      setting.gaElementTypes.addOne(toGradeType(eleType))
    }
    obj.get("noMakeupExamStatuses").asInstanceOf[ju.List[ju.Map[String, Any]]].asScala foreach { eleType =>
      setting.noMakeupExamStatuses.addOne(toExamStatus(eleType))
    }
    obj.get("emptyScoreStatuses").asInstanceOf[ju.List[ju.Map[String, Any]]].asScala foreach { eleType =>
      setting.emptyScoreStatuses.addOne(toExamStatus(eleType))
    }
    obj.get("noMakeupTakeTypes").asInstanceOf[ju.List[ju.Map[String, Any]]].asScala foreach { eleType =>
      setting.noMakeupTakeTypes.addOne(toCourseTakeType(eleType))
    }
    setting.submitIsPublish = obj.get("submitIsPublish").asInstanceOf[java.lang.Boolean].booleanValue()
    setting
  }

  private def toGradeType(data: ju.Map[String, Any]): GradeType = {
    val gt = new GradeType(data.get("id").asInstanceOf[Number].intValue)
    gt.name = data.get("name").toString
    gt
  }

  private def toExamStatus(data: ju.Map[String, Any]): ExamStatus = {
    val status = new ExamStatus(data.get("id").asInstanceOf[Number].intValue)
    status.name = data.get("name").toString
    status
  }

  private def toCourseTakeType(data: ju.Map[String, Any]): CourseTakeType = {
    new CourseTakeType(data.get("id").asInstanceOf[Number].intValue, null, data.get("name").toString, null)
  }

  def main(args: Array[String]): Unit = {
    val setting = parse("""{"gaElementTypes":[{"id":2,"name":"期末成绩"},{"id":3,"name":"平时成绩"}],"noMakeupExamStatuses":[{"id":2,"name":"缓考"},{"id":3,"name":"缺考"}],"noMakeupTakeTypes":[{"name":"重修","id":3}],"emptyScoreStatuses":[{"id":2,"name":"缓考"},{"id":3,"name":"缺考"}],"submitIsPublish":false}""")
    println(setting.gaElementTypes.size)
  }
}
