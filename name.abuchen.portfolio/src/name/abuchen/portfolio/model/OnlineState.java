package name.abuchen.portfolio.model;

import java.util.EnumMap;
import java.util.Map;

import name.abuchen.portfolio.Messages;

public class OnlineState
{
    public enum Property
    {
        NAME(Messages.CSVColumn_SecurityName), //
        ISIN(Messages.CSVColumn_ISIN), //
        WKN(Messages.CSVColumn_WKN), //
        TICKER(Messages.CSVColumn_TickerSymbol);

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

    public enum State
    {
        BLANK, SYNCED, CUSTOM, EDITED;
    }

    private Map<Property, State> state = new EnumMap<>(Property.class);

    public State getState(Property property)
    {
        State answer = state.get(property);
        return answer == null ? State.BLANK : answer;
    }

    public State setState(Property property, State state)
    {
        return this.state.put(property, state);
    }
}
