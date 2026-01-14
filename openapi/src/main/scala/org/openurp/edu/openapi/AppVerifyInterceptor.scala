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

package org.openurp.edu.openapi

import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.beangle.ems.app.security.RemoteService
import org.beangle.web.servlet.intercept.Interceptor
import org.beangle.webmvc.context.ActionContext

object AppVerifyInterceptor extends Interceptor {

  override def preInvoke(request: HttpServletRequest, response: HttpServletResponse): Boolean = {
    val authorizationHeader = request.getHeader("Authorization")

    // 1. 校验Authorization格式：必须是 Bearer + 空格 + 令牌
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
      response.setContentType("application/json;charset=UTF-8")
      response.getWriter.write("{\"code\":401,\"msg\":\"未携带有效JWT令牌\"}")
      return false
    }

    // 3. 提取JWT令牌（去掉"Bearer "前缀）
    val token = authorizationHeader.substring(7)
    if (!RemoteService.verifyJwtToken(token)) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
      response.setContentType("application/json;charset=UTF-8")
      response.getWriter.write("{\"code\":401,\"msg\":\"JWT令牌无效或已过期\"}")
      false
    } else {
      true
    }
  }

  override def postInvoke(request: HttpServletRequest, response: HttpServletResponse): Unit = {}
}
