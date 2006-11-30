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

import java.io.IOException;
import java.io.Reader;

import org.alfresco.error.AlfrescoRuntimeException;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;

/**
 * Simple tokeniser for longs.
 * 
 * @author Andy Hind
 */
public class LongTokenFilter extends Tokenizer
{
    Tokenizer baseTokeniser;
    
    public LongTokenFilter(Reader in)
    {
        super(in);
        baseTokeniser = new WhitespaceTokenizer(in);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.analysis.TokenStream#next()
     */

    public Token next() throws IOException
    {
        Token candidate;
        while((candidate = baseTokeniser.next()) != null)
        {
            try
            {
                Long longValue = Long.valueOf(candidate.termText());
                String valueString = NumericEncoder.encode(longValue.longValue());
                Token longToken = new Token(valueString, candidate.startOffset(), candidate.startOffset(),
                        candidate.type());
                return longToken;
            }
            catch (NumberFormatException e)
            {
                // just ignore and try the next one
            }
        }
        return null;
    }
}