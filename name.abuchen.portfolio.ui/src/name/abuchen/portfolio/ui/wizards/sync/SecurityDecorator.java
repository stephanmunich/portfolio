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
import java.util.Optional;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.model.Adaptable;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.OnlineState;
import name.abuchen.portfolio.model.OnlineState.Property;
import name.abuchen.portfolio.model.OnlineState.State;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.PortfolioPlugin;

public class SecurityDecorator implements Adaptable
{
    private static final String SEARCH_URL = "http://testapi.fabritius.org/securities/search/{0}"; //$NON-NLS-1$
    private static final String READ_URL = "http://testapi.fabritius.org/securities/{0}"; //$NON-NLS-1$

    private Security security;

    private String onlineId;

    private Map<OnlineState.Property, OnlineProperty> properties = new EnumMap<>(Property.class);

    public SecurityDecorator(Security security)
    {
        this.security = security;

        this.onlineId = security.getOnlineId();

        OnlineState state = security.getOnlineState();

        properties.put(Property.NAME, new OnlineProperty(security.getName(), state.getState(Property.NAME)));
        properties.put(Property.ISIN, new OnlineProperty(security.getIsin(), state.getState(Property.ISIN)));
        properties.put(Property.WKN, new OnlineProperty(security.getWkn(), state.getState(Property.WKN)));
        properties.put(Property.TICKER,
                        new OnlineProperty(security.getTickerSymbol(), state.getState(Property.TICKER)));
    }

    public OnlineProperty getProperty(Property property)
    {
        return properties.get(property);
    }

    public Security getSecurity()
    {
        return security;
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
            if (onlineId != null)
                updateSyncedItem();
            else
                searchOnline();
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
        }
    }

    private void updateSyncedItem() throws IOException
    {
        readOnline().ifPresent(onlineItem -> {
            properties.get(Property.NAME).setSuggestedValue(onlineItem.getName());
            properties.get(Property.ISIN).setSuggestedValue(onlineItem.getIsin());
            properties.get(Property.WKN).setSuggestedValue(onlineItem.getWkn());
            properties.get(Property.TICKER).setSuggestedValue(onlineItem.getTicker());
        });
    }

    private Optional<OnlineItem> readOnline() throws IOException
    {
        String searchUrl = MessageFormat.format(READ_URL, URLEncoder.encode(onlineId, StandardCharsets.UTF_8.name()));

        try (Scanner scanner = new Scanner(new URL(searchUrl).openStream(), StandardCharsets.UTF_8.name()))
        {
            String html = scanner.useDelimiter("\\A").next(); //$NON-NLS-1$

            JSONObject response = (JSONObject) JSONValue.parse(html);
            if (response != null)
                return Optional.of(OnlineItem.from(response));
        }

        return Optional.empty();
    }

    public void searchOnline() throws IOException
    {

        List<OnlineItem> onlineItems = loadOnline();
        if (onlineItems.isEmpty())
            return;

        // assumption: first in the list is the "best" match

        OnlineItem onlineItem = onlineItems.get(0);

        this.onlineId = onlineItem.getId();

        properties.get(Property.NAME).setSuggestedValue(onlineItem.getName());
        properties.get(Property.ISIN).setSuggestedValue(onlineItem.getIsin());
        properties.get(Property.WKN).setSuggestedValue(onlineItem.getWkn());
        properties.get(Property.TICKER).setSuggestedValue(onlineItem.getTicker());

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

        String searchUrl = MessageFormat.format(SEARCH_URL,
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

    /**
     * Applies selected changes to the original security.
     * 
     * @return true if any changes were made.
     */
    public boolean apply()
    {
        if (onlineId == null)
            return false;

        boolean[] isDirty = new boolean[1];

        if (!onlineId.equals(security.getOnlineId()))
        {
            security.setOnlineId(onlineId);
            isDirty[0] = true;
        }

        properties.entrySet().stream().forEach(entry -> {
            if (entry.getValue().isModified())
            {
                switch (entry.getKey())
                {
                    case NAME:
                        security.setName(entry.getValue().getSuggestedValue());
                        break;
                    case ISIN:
                        security.setIsin(entry.getValue().getSuggestedValue());
                        break;
                    case WKN:
                        security.setWkn(entry.getValue().getSuggestedValue());
                        break;
                    case TICKER:
                        security.setTickerSymbol(entry.getValue().getSuggestedValue());
                        break;
                    default:
                }

                isDirty[0] = true;
            }

            State newState = entry.getValue().getSuggestedState();
            State oldState = security.getOnlineState().setState(entry.getKey(), newState);

            isDirty[0] = isDirty[0] || !newState.equals(oldState);
        });

        return isDirty[0];
    }

}
