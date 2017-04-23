package name.abuchen.portfolio.ui.wizards.sync;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.wizards.sync.OnlineProperty.Property;

public class OnlinePropertyColumn extends Column
{
    public static class NameColumnLabelProvider extends ColumnLabelProvider
    {
        private final Property property;

        public NameColumnLabelProvider(Property property)
        {
            this.property = property;
        }

        @Override
        public String getText(Object e)
        {
            OnlineProperty p = ((OnlineSecurity) e).getProperty(property);
            return p.getValue();
        }

        @Override
        public Color getBackground(Object e)
        {
            OnlineProperty p = ((OnlineSecurity) e).getProperty(property);
            return p.isModified() ? Display.getDefault().getSystemColor(SWT.COLOR_GREEN) : null;
        }
    }

    public OnlinePropertyColumn(Property property, int defaultWidth)
    {
        super(property.name(), property.getLabel(), SWT.LEFT, defaultWidth);

        setLabelProvider(new NameColumnLabelProvider(property));
        setSorter(ColumnViewerSorter.create(e -> ((OnlineSecurity) e).getProperty(property).getValue()));
    }
}
