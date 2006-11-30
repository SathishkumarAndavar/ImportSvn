/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.search.impl.lucene.analysis;

/**
 * Simple analyser to wrap the tokenisation of doubles.
 * 
 * @author Andy Hind
 */
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;

public class DoubleAnalyser extends Analyzer
{

    public DoubleAnalyser()
    {
        super();
    }


    public TokenStream tokenStream(String fieldName, Reader reader)
    {
        return new DoubleTokenFilter(reader);
    }
}
