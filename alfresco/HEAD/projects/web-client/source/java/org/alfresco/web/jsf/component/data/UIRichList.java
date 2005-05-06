/*
 * Created on Mar 11, 2005
 */
package org.alfresco.web.jsf.component.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.apache.log4j.Logger;

import org.alfresco.web.data.IDataContainer;
import org.alfresco.web.jsf.renderer.data.IRichListRenderer;
import org.alfresco.web.jsf.renderer.data.RichListRenderer;

/**
 * @author kevinr
 */
public class UIRichList extends UIComponentBase implements IDataContainer
{
   // ------------------------------------------------------------------------------
   // Construction
   
   /**
    * Default constructor
    */
   public UIRichList()
   {
      setRendererType("awc.faces.RichListRenderer");
      
      // the standard set of view renderers
      IRichListRenderer renderer;
      
      renderer = new RichListRenderer.IconViewRenderer();
      UIRichList.viewRenderers.put(renderer.getViewModeID(), renderer);
      
      renderer = new RichListRenderer.DetailsViewRenderer();
      UIRichList.viewRenderers.put(renderer.getViewModeID(), renderer);
      
      renderer = new RichListRenderer.ListViewRenderer();
      UIRichList.viewRenderers.put(renderer.getViewModeID(), renderer);
   }


   // ------------------------------------------------------------------------------
   // Component implementation
   
   /**
    * @see javax.faces.component.UIComponent#getFamily()
    */
   public String getFamily()
   {
      return "awc.faces.Data";
   }
   
   /**
    * @see javax.faces.component.StateHolder#restoreState(javax.faces.context.FacesContext, java.lang.Object)
    */
   public void restoreState(FacesContext context, Object state)
   {
      Object values[] = (Object[])state;
      // standard component attributes are restored by the super class
      super.restoreState(context, values[0]);
      this.currentPage = ((Integer)values[1]).intValue();
      this.sortColumn = (String)values[2];
      this.sortDescending = ((Boolean)values[3]).booleanValue();
   }
   
   /**
    * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
    */
   public Object saveState(FacesContext context)
   {
      Object values[] = new Object[4];
      // standard component attributes are saved by the super class
      values[0] = super.saveState(context);
      values[1] = Integer.valueOf(this.currentPage);
      values[2] = this.sortColumn;
      values[3] = (this.sortDescending ? Boolean.TRUE : Boolean.FALSE);
      return (values);
   }
   
   /**
    * Get the value (for this component the value is an object used as the DataModel)
    *
    * @return the value
    */
   public Object getValue()
   {
      if (this.value == null)
      {
         ValueBinding vb = getValueBinding("value");
         if (vb != null)
         {
            this.value = vb.getValue(getFacesContext());
         }
      }
      return this.value;
   }

   /**
    * Set the value (for this component the value is an object used as the DataModel)
    *
    * @param value     the value
    */
   public void setValue(Object value)
   {
      this.dataModel = null;
      this.value = value;
   }
   
   /**
    * Get the view mode for this Rich List
    * 
    * @return view mode as a String
    */
   public String getViewMode()
   {
      ValueBinding vb = getValueBinding("viewMode");
      if (vb != null)
      {
         this.viewMode = (String)vb.getValue(getFacesContext());
      }
      
      return this.viewMode;
   }
   
   /**
    * Set the current view mode for this Rich List
    * 
    * @param viewMode      the view mode as a String
    */
   public void setViewMode(String viewMode)
   {
      this.viewMode = viewMode;
   }
   
   
   // ------------------------------------------------------------------------------
   // IDataContainer implementation 
   
   /**
    * Return the currently sorted column if any
    * 
    * @return current sorted column if any
    */
   public String getCurrentSortColumn()
   {
      return this.sortColumn;
   }
   
   /**
    * @see org.alfresco.web.data.IDataContainer#isCurrentSortDescending()
    */
   public boolean isCurrentSortDescending()
   {
      return this.sortDescending;
   }
   
   /**
    * Returns the current page size used for this list, or -1 for no paging.
    */
   public int getPageSize()
   {
      return this.pageSize;
   }
   
   /**
    * Sets the current page size used for the list.
    * 
    * @param val
    */
   public void setPageSize(int val)
   {
      // TODO: value binding code
      this.pageSize = val;
   }
   
   /**
    * @see org.alfresco.web.data.IDataContainer#getPageCount()
    */
   public int getPageCount()
   {
      return this.pageCount;
   }
   
   /**
    * Return the current page the list is displaying
    * 
    * @return current page zero based index
    */
   public int getCurrentPage()
   {
      return this.currentPage;
   }
   
   /**
    * @see org.alfresco.web.data.IDataContainer#setCurrentPage(int)
    */
   public void setCurrentPage(int index)
   {
      this.currentPage = index;
   }

   /**
    * Returns true if a row of data is available
    * 
    * @return true if data is available, false otherwise
    */
   public boolean isDataAvailable()
   {
      return this.rowIndex < this.maxRowIndex;
   }
   
   /**
    * Returns the next row of data from the data model
    * 
    * @return next row of data as a Bean object
    */
   public Object nextRow()
   {
      // get next row and increment row count
      Object rowData = getDataModel().getRow(this.rowIndex + 1);
      
      // Prepare the data-binding variable "var" ready for the next cycle of
      // renderering for the child components. 
      String var = (String)getAttributes().get("var");
      if (var != null)
      {
         Map requestMap = getFacesContext().getExternalContext().getRequestMap();
         if (isDataAvailable() == true)
         {
            requestMap.put(var, rowData);
         }
         else
         {
            requestMap.remove(var);
         }
      }
      
      this.rowIndex++;
      
      return rowData;
   }
   
   /**
    * Sort the dataset using the specified sort parameters
    * 
    * @param column        Column to sort
    * @param descending    True for descending sort, false for ascending
    * @param mode          Sort mode to use (see IDataContainer constants)
    */
   public void sort(String column, boolean descending, String mode)
   {
      this.sortColumn = column;
      this.sortDescending = descending;
      
      // delegate to the data model to sort its contents
      getDataModel().sort(column, descending, mode);
   }
   
   
   // ------------------------------------------------------------------------------
   // UIRichList implementation
   
   /**
    * Method called to bind the RichList component state to the data model value
    */
   public void bind()
   {
      int rowCount = getDataModel().size();
      // if a page size is specified, then we use that
      if (this.pageSize != -1)
      {
         // calc start row index based on current page index
         this.rowIndex = (this.currentPage * this.pageSize) - 1;
         
         // calc total number of pages available
         this.pageCount = (rowCount / this.pageSize) + 1;
         if (rowCount % this.pageSize == 0 && this.pageCount != 1)
         {
            this.pageCount--;
         }
         
         // calc the maximum row index that can be returned
         this.maxRowIndex = this.rowIndex + this.pageSize;
         if (this.maxRowIndex >= rowCount)
         {
            this.maxRowIndex = rowCount - 1;
         }
      }
      // else we are not paged so show all data from start
      else
      {
         this.rowIndex = -1;
         this.pageCount = 1;
         this.maxRowIndex = (rowCount - 1);
      }
      //if (s_logger.isDebugEnabled())
      //   s_logger.debug("Bound datasource: PageSize: " + this.pageSize + "; CurrentPage: " + this.currentPage + "; RowIndex: " + this.rowIndex + "; MaxRowIndex: " + this.maxRowIndex + "; RowCount: " + rowCount);
   }
   
   /**
    * @return A new IRichListRenderer implementation for the current view mode
    */
   public IRichListRenderer getViewRenderer()
   {
      // get type from current view mode, then create an instance of the renderer
      // TODO: set the appropriate IRichListRenderer impl - could come from a config?
      //       should allow custom views to be specified in config etc.
      IRichListRenderer renderer = null;
      if (getViewMode() != null)
      {
         renderer = (IRichListRenderer)UIRichList.viewRenderers.get(getViewMode());
      }
      return renderer;
   }
   
   /**
    * Return the data model wrapper
    * 
    * @return IGridDataModel 
    */
   private IGridDataModel getDataModel()
   {
      if (this.dataModel == null)
      {
         // build the appropriate data-model wrapper object
         Object val = getValue();
         if (val instanceof List)
         {
            this.dataModel = new GridListDataModel((List)val);
         }
         else if ( (java.lang.Object[].class).isAssignableFrom(val.getClass()) )
         {
            this.dataModel = new GridArrayDataModel((Object[])val);
         }
         else
         {
            throw new IllegalStateException("UIRichList 'value' attribute binding should specify data model of a supported type!"); 
         }
         
         // sort first time on initially sorted column if set
         String initialSortColumn = (String)getAttributes().get("initialSortColumn");
         if (initialSortColumn != null && initialSortColumn.length() != 0)
         {
            boolean descending = true;
            if (getAttributes().get("initialSortDescending") != null)
            {
               descending = ((Boolean)getAttributes().get("initialSortDescending")).booleanValue();
            }
            // TODO: add support for retrieving correct column sort mode here
            this.sortColumn = initialSortColumn;
            this.sortDescending = descending;
            
            // delegate to the data model to sort its contents
            this.dataModel.sort(initialSortColumn, descending, IDataContainer.SORT_CASEINSENSITIVE);
         }
      }
      
      return this.dataModel;
   }
   
   
   // ------------------------------------------------------------------------------
   // Private data
   
   // component state
   private int currentPage = 0;
   private String sortColumn = null;
   private boolean sortDescending = true;
   private Object value = null;
   private final static Map<String, IRichListRenderer> viewRenderers = new HashMap<String, IRichListRenderer>(5);
   
   // transient component state that exists during a single page refresh only
   private int rowIndex = -1;
   private int maxRowIndex = -1;
   private IGridDataModel dataModel = null;
   
   // component settings set by tag
   private int pageSize = -1;
   private int pageCount = 1;
   private String viewMode = null;
   
   private static Logger s_logger = Logger.getLogger(IDataContainer.class);
}
