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

package org.openurp.edu.grade.service.event

import org.beangle.commons.event.BusinessEvent
import org.openurp.edu.grade.model.CourseGrade

/**
 * 已发布成绩变化事件(由于已发布成绩发生变化如修改申请或加分等引起的成绩改变的事件)
 */
@SerialVersionUID(-3680027610530167290L)
class CourseGradeModifyEvent(source: Seq[CourseGrade]) extends BusinessEvent(source)
