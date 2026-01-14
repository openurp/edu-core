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

import org.beangle.commons.json.{Json, JsonObject}
import org.beangle.commons.lang.Strings
import org.openurp.base.model.Project
import org.openurp.base.service.ProjectConfigService
import org.openurp.code.edu.model.{CourseTakeType, ExamStatus, GradeType}
import org.openurp.code.service.CodeService
import org.openurp.edu.grade.service.{BaseServiceImpl, CourseGradeSetting, CourseGradeSettings}

class CourseGradeSettingsImpl extends BaseServiceImpl, CourseGradeSettings {

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
    val obj = Json.parseObject(str)
    val setting = new CourseGradeSetting()
    obj.getArray("gaElementTypes") foreach { ele =>
      setting.gaElementTypes.addOne(toGradeType(ele.asInstanceOf[JsonObject]))
    }
    obj.getArray("noMakeupExamStatuses") foreach { ele =>
      setting.noMakeupExamStatuses.addOne(toExamStatus(ele.asInstanceOf[JsonObject]))
    }
    obj.getArray("emptyScoreStatuses") foreach { ele =>
      setting.emptyScoreStatuses.addOne(toExamStatus(ele.asInstanceOf[JsonObject]))
    }
    obj.getArray("noMakeupTakeTypes").foreach { ele =>
      setting.noMakeupTakeTypes.addOne(toCourseTakeType(ele.asInstanceOf[JsonObject]))
    }
    setting.submitIsPublish = obj.getBoolean("submitIsPublish", false)
    setting
  }

  private def toGradeType(ele: JsonObject): GradeType = {
    val gt = new GradeType(ele.getInt("id"))
    gt.name = ele.getString("name")
    gt
  }

  private def toExamStatus(elem: JsonObject): ExamStatus = {
    val status = new ExamStatus(elem.getInt("id"))
    status.name = elem.getString("name")
    status
  }

  private def toCourseTakeType(elem: JsonObject): CourseTakeType = {
    new CourseTakeType(elem.getInt("id"), null, elem.getString("name"), null)
  }
}
