package name.abuchen.portfolio.ui.wizards.sync;

import org.json.simple.JSONObject;

class OnlineItem
{
    private String id;
    private String name;
    private String isin;
    private String wkn;
    private String ticker;

    public static OnlineItem from(JSONObject json)
    {
        OnlineItem vehicle = new OnlineItem();
        vehicle.id = (String) json.get("id"); //$NON-NLS-1$
        vehicle.name = (String) json.get("name"); //$NON-NLS-1$
        vehicle.isin = (String) json.get("isin"); //$NON-NLS-1$
        vehicle.wkn = (String) json.get("wkn"); //$NON-NLS-1$
        vehicle.ticker = (String) json.get("ticker"); //$NON-NLS-1$
        return vehicle;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getIsin()
    {
        return isin;
    }

    public String getWkn()
    {
        return wkn;
    }

    public String getTicker()
    {
        return ticker;
    }
}