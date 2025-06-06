/*
 * IGinX - the polystore system with high performance
 * Copyright (C) Tsinghua University
 * TSIGinX@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package cn.edu.tsinghua.iginx.sql.statement;

import cn.edu.tsinghua.iginx.IginxWorker;
import cn.edu.tsinghua.iginx.engine.shared.RequestContext;
import cn.edu.tsinghua.iginx.engine.shared.Result;
import cn.edu.tsinghua.iginx.engine.shared.exception.StatementExecutionException;
import cn.edu.tsinghua.iginx.thrift.AuthType;
import cn.edu.tsinghua.iginx.thrift.Status;
import cn.edu.tsinghua.iginx.thrift.UpdateUserReq;
import java.util.Set;

public class GrantUserStatement extends SystemStatement {
  private final String username;
  private final Set<AuthType> authTypes;

  public GrantUserStatement(String username, Set<AuthType> authTypes) {
    this.statementType = StatementType.GRANT_USER;
    this.username = username;
    this.authTypes = authTypes;
  }

  public String getUsername() {
    return username;
  }

  public Set<AuthType> getAuthTypes() {
    return authTypes;
  }

  @Override
  public void execute(RequestContext ctx) throws StatementExecutionException {
    IginxWorker worker = IginxWorker.getInstance();
    UpdateUserReq req = new UpdateUserReq(ctx.getSessionId(), username);
    if (authTypes != null && !authTypes.isEmpty()) {
      req.setAuths(authTypes);
    }
    Status status = worker.updateUser(req);
    ctx.setResult(new Result(status));
  }
}
