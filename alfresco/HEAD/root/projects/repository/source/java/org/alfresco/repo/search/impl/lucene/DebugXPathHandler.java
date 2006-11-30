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
package org.alfresco.repo.search.impl.lucene;

import org.saxpath.Axis;
import org.saxpath.SAXPathException;
import org.saxpath.XPathHandler;

import com.werken.saxpath.XPathReader;

public class DebugXPathHandler implements XPathHandler
{

    public DebugXPathHandler()
    {
        super();
        // TODO Auto-generated constructor stub
    }

    public void endAbsoluteLocationPath() throws SAXPathException
    {
        System.out.println("End Absolute Location Path");
    }

    public void endAdditiveExpr(int arg0) throws SAXPathException
    {
        System.out.println("End Additive Expr: value = " + arg0);
    }

    public void endAllNodeStep() throws SAXPathException
    {
        System.out.println("End All Node Step");
    }

    public void endAndExpr(boolean arg0) throws SAXPathException
    {
        System.out.println("End And Expr: value = " + arg0);
    }

    public void endCommentNodeStep() throws SAXPathException
    {
        System.out.println("End Comment Node Step");
    }

    public void endEqualityExpr(int arg0) throws SAXPathException
    {
        System.out.println("End Equality Expr: value = " + arg0);
    }

    public void endFilterExpr() throws SAXPathException
    {
        System.out.println("End Filter Expr");
    }

    public void endFunction() throws SAXPathException
    {
        System.out.println("End Function");
    }

    public void endMultiplicativeExpr(int arg0) throws SAXPathException
    {
        System.out.println("End Multiplicative Expr: value = " + arg0);
    }

    public void endNameStep() throws SAXPathException
    {
        System.out.println("End Name Step");
    }

    public void endOrExpr(boolean arg0) throws SAXPathException
    {
        System.out.println("End Or Expr: value = " + arg0);
    }

    public void endPathExpr() throws SAXPathException
    {
        System.out.println("End Path Expression");
    }

    public void endPredicate() throws SAXPathException
    {
        System.out.println("End Predicate");
    }

    public void endProcessingInstructionNodeStep() throws SAXPathException
    {
        System.out.println("End Processing Instruction Node Step");
    }

    public void endRelationalExpr(int arg0) throws SAXPathException
    {
        System.out.println("End Relational Expr: value = " + arg0);
    }

    public void endRelativeLocationPath() throws SAXPathException
    {
        System.out.println("End Relative Location Path");
    }

    public void endTextNodeStep() throws SAXPathException
    {
        System.out.println("End Text Node Step");
    }

    public void endUnaryExpr(int arg0) throws SAXPathException
    {
        System.out.println("End Unary Expr: value = " + arg0);
    }

    public void endUnionExpr(boolean arg0) throws SAXPathException
    {
        System.out.println("End Union Expr: value = " + arg0);
    }

    public void endXPath() throws SAXPathException
    {
        System.out.println("End XPath");
    }

    public void literal(String arg0) throws SAXPathException
    {
        System.out.println("Literal = " + arg0);
    }

    public void number(double arg0) throws SAXPathException
    {
        System.out.println("Double = " + arg0);
    }

    public void number(int arg0) throws SAXPathException
    {
        System.out.println("Integer = " + arg0);
    }

    public void startAbsoluteLocationPath() throws SAXPathException
    {
        System.out.println("Start Absolute Location Path");
    }

    public void startAdditiveExpr() throws SAXPathException
    {
        System.out.println("Start Additive Expression");
    }

    public void startAllNodeStep(int arg0) throws SAXPathException
    {
        System.out.println("Start All Node Exp: Axis = " + Axis.lookup(arg0));
    }

    public void startAndExpr() throws SAXPathException
    {
        System.out.println("Start AndExpression");
    }

    public void startCommentNodeStep(int arg0) throws SAXPathException
    {
        System.out.println("Start Comment Node Step");
    }

    public void startEqualityExpr() throws SAXPathException
    {
        System.out.println("Start Equality Expression");
    }

    public void startFilterExpr() throws SAXPathException
    {
        System.out.println("Start Filter Expression");
    }

    public void startFunction(String arg0, String arg1) throws SAXPathException
    {
        System.out.println("Start Function arg0 = < " + arg0 + " > arg1 = < " + arg1 + " >");
    }

    public void startMultiplicativeExpr() throws SAXPathException
    {
        System.out.println("Start Multiplicative Expression");
    }

    public void startNameStep(int arg0, String arg1, String arg2) throws SAXPathException
    {
        System.out.println("Start Name Step Axis = <" + Axis.lookup(arg0) + " > arg1 = < " + arg1 + " > arg 2 <" + arg2
                + " >");
    }

    public void startOrExpr() throws SAXPathException
    {
        System.out.println("Start Or Expression");
    }

    public void startPathExpr() throws SAXPathException
    {
        System.out.println("Start Path Expression");
    }

    public void startPredicate() throws SAXPathException
    {
        System.out.println("Start Predicate");
    }

    public void startProcessingInstructionNodeStep(int arg0, String arg1) throws SAXPathException
    {
        System.out.println("Start Processing INstruction Node Step = < " + arg0 + " > arg1 = < " + arg1 + " >");
    }

    public void startRelationalExpr() throws SAXPathException
    {
        System.out.println("Start Relationship Expression");
    }

    public void startRelativeLocationPath() throws SAXPathException
    {
        System.out.println("Start Relative Location Path");
    }

    public void startTextNodeStep(int arg0) throws SAXPathException
    {
        System.out.println("Start Text Node Step: value = " + arg0);
    }

    public void startUnaryExpr() throws SAXPathException
    {
        System.out.println("Start Unary Expression");
    }

    public void startUnionExpr() throws SAXPathException
    {
        System.out.println("Start Union Expression");
    }

    public void startXPath() throws SAXPathException
    {
        System.out.println("Start XPath");
    }

    public void variableReference(String arg0, String arg1) throws SAXPathException
    {
        System.out.println("Variable Reference arg0 = < " + arg0 + " > arg1 = < " + arg1);
    }

    /**
     * @param args
     * @throws SAXPathException
     */
    public static void main(String[] args) throws SAXPathException
    {
        XPathReader reader = new XPathReader();
        reader.setXPathHandler(new DebugXPathHandler());
        reader
                .parse("/ns:one[@woof='dog']/two/./../two[functionTest(@a, @b, $woof:woof)]/three/*/four//*/five/six[@exists1 and @exists2]");
    }

}
