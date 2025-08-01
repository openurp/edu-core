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

package org.openurp.edu.program.util

import org.beangle.commons.text.seq.SeqNumStyle.{ARABIC, HANZI}
import org.beangle.commons.text.seq.{MultiLevelSeqGenerator, SeqPattern}

object SeqHelper {

  def getGenerator: MultiLevelSeqGenerator = {
    val sg = new MultiLevelSeqGenerator
    // 'A2','A3','B1','B2','B3','C1','C2','C3','D1','D2','D3','F'
    sg.add(new SeqPattern(ARABIC, "{1}"))
    sg.add(new SeqPattern(ARABIC, "{1}.{2}"))
    sg.add(new SeqPattern(ARABIC, "{1}.{2}.{3}"))
    sg.add(new SeqPattern(ARABIC, "{1}.{2}.{3}.{4}"))
    sg.add(new SeqPattern(ARABIC, "{1}.{2}.{3}.{4}.{5}"))
    sg.add(new SeqPattern(ARABIC, "{1}.{2}.{3}.{4}.{5}.{6}"))
    sg.add(new SeqPattern(ARABIC, "{1}.{2}.{3}.{4}.{5}.{6}.{7}"))
    sg.add(new SeqPattern(ARABIC, "{1}.{2}.{3}.{4}.{5}.{6}.{7}.{8}"))
    sg.add(new SeqPattern(ARABIC, "{1}.{2}.{3}.{4}.{5}.{6}.{7}.{8}.{9}"))
    sg
  }
}
