package name.abuchen.portfolio.ui.wizards.sync;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.model.Adaptable;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.wizards.sync.OnlineProperty.Property;

public class OnlineSecurity implements Adaptable
{
    private static final String SOURCE_URL = "http://testapi.fabritius.org/securities/search/{0}"; //$NON-NLS-1$

    private Security security;
    private Map<Property, OnlineProperty> properties = new EnumMap<>(Property.class);
    private List<OnlineItem> onlineItems = Collections.emptyList();

    public OnlineSecurity(Security security)
    {
        this.security = security;

        properties.put(Property.ID, new OnlineProperty(security.getOnlineId()));
        properties.put(Property.NAME, new OnlineProperty(security.getName()));
        properties.put(Property.ISIN, new OnlineProperty(security.getIsin()));
        properties.put(Property.WKN, new OnlineProperty(security.getWkn()));
        properties.put(Property.TICKER, new OnlineProperty(security.getTickerSymbol()));
    }

    public OnlineProperty getProperty(Property property)
    {
        return properties.get(property);
    }

    public Security getSecurity()
    {
        return security;
    }

    public List<OnlineItem> getOnlineItems()
    {
        return onlineItems;
    }

    @Override
    public <T> T adapt(Class<T> type)
    {
        if (type == Named.class)
            return type.cast(security);
        else
            return null;
    }

    public void checkOnline()
    {
        try
        {
            this.onlineItems = loadOnline();
            if (onlineItems.isEmpty())
                return;

            // assumption: first in the list is the "best" match

            OnlineItem onlineItem = onlineItems.get(0);

            properties.get(Property.ID).setSuggestedValue(onlineItem.getId());
            properties.get(Property.NAME).setSuggestedValue(onlineItem.getName());
            properties.get(Property.ISIN).setSuggestedValue(onlineItem.getIsin());
            properties.get(Property.WKN).setSuggestedValue(onlineItem.getWkn());
            properties.get(Property.TICKER).setSuggestedValue(onlineItem.getTicker());
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
        }
    }

    private List<OnlineItem> loadOnline() throws IOException
    {
        String searchProperty = security.getIsin();
        if (searchProperty == null || searchProperty.isEmpty())
            searchProperty = security.getWkn();
        if (searchProperty == null || searchProperty.isEmpty())
            searchProperty = security.getTickerSymbol();
        if (searchProperty == null || searchProperty.isEmpty())
            searchProperty = security.getName();

        if (searchProperty == null || searchProperty.isEmpty())
            return Collections.emptyList();

        String searchUrl = MessageFormat.format(SOURCE_URL,
                        URLEncoder.encode(searchProperty, StandardCharsets.UTF_8.name()));

        List<OnlineItem> answer = new ArrayList<>();

        try (Scanner scanner = new Scanner(new URL(searchUrl).openStream(), StandardCharsets.UTF_8.name()))
        {
            String html = scanner.useDelimiter("\\A").next(); //$NON-NLS-1$

            JSONArray response = (JSONArray) JSONValue.parse(html);
            if (response != null)
            {
                for (int ii = 0; ii < response.size(); ii++)
                    answer.add(OnlineItem.from((JSONObject) response.get(ii)));
            }
        }

        return answer;
    }

}
