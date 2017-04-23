package name.abuchen.portfolio.ui.wizards.sync;

import name.abuchen.portfolio.ui.Messages;

public class OnlineProperty
{
    public enum Property
    {
        ID("Online Id"), NAME(Messages.ColumnName), ISIN(Messages.ColumnISIN), WKN(Messages.ColumnWKN), TICKER(
                        Messages.ColumnTicker);

        private String label;

        private Property(String label)
        {
            this.label = label;
        }

        public String getLabel()
        {
            return label;
        }
    }

    private String originalValue;
    private String suggestedValue;

    private boolean isModified;

    public OnlineProperty(String originalValue)
    {
        this.originalValue = originalValue;
    }

    public String getOriginalValue()
    {
        return originalValue;
    }

    public String getSuggestedValue()
    {
        return suggestedValue;
    }

    public void setSuggestedValue(String suggestedValue)
    {
        this.suggestedValue = suggestedValue;

        if (suggestedValue != null && !suggestedValue.equals(originalValue))
            isModified = true;
    }

    public String getValue()
    {
        return isModified ? suggestedValue : originalValue;
    }

    public boolean isModified()
    {
        return isModified;
    }

    public void setModified(boolean isModified)
    {
        this.isModified = isModified;
    }
}
