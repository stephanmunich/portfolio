package name.abuchen.portfolio.ui.wizards.sync;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;
import name.abuchen.portfolio.ui.wizards.sync.OnlineProperty.Property;

public class SyncMasterDataPage extends AbstractWizardPage
{
    private final IPreferenceStore preferences;

    private List<OnlineSecurity> securities;

    private TableViewer tableViewer;

    public SyncMasterDataPage(Client client, IPreferenceStore preferences)
    {
        super("syncpage"); //$NON-NLS-1$
        this.preferences = preferences;

        this.securities = client.getSecurities().stream().map(OnlineSecurity::new).collect(Collectors.toList());

        setTitle("Stammdatenabgleich");
        setMessage("Grün markierte Wertpapiere werden aktualisiert. Per Kontextmenü den passenden Wert auswählen.");
    }

    @Override
    public void beforePage()
    {
        Display.getDefault().asyncExec(this::runOnlineSync);
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);

        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        tableViewer = new TableViewer(container, SWT.FULL_SELECTION | SWT.SINGLE);
        ColumnViewerToolTipSupport.enableFor(tableViewer, ToolTip.NO_RECREATE);

        ShowHideColumnHelper support = new ShowHideColumnHelper(SyncMasterDataPage.class.getSimpleName() + "@start2",
                        preferences, tableViewer, layout);

        addColumns(support);
        support.createColumns();

        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        new ContextMenu(tableViewer.getTable(), this::fillContextMenu).hook();

        setControl(container);
    }

    private void addColumns(ShowHideColumnHelper support)
    {
        support.addColumn(new OnlinePropertyColumn(Property.NAME, 250));
        support.addColumn(new OnlinePropertyColumn(Property.ISIN, 120));
        support.addColumn(new OnlinePropertyColumn(Property.WKN, 120));
        support.addColumn(new OnlinePropertyColumn(Property.TICKER, 120));
    }

    private void fillContextMenu(IMenuManager menuManager)
    {
        OnlineSecurity delegate = (OnlineSecurity) tableViewer.getStructuredSelection().getFirstElement();
        if (delegate == null)
            return;

        for (Property p : Property.values())
        {
            OnlineProperty onlineProperty = delegate.getProperty(p);

            MenuManager menu = new MenuManager(p.getLabel());
            menuManager.add(menu);

            SimpleAction action = new SimpleAction(onlineProperty.getOriginalValue(), a -> {
                onlineProperty.setModified(false);
                tableViewer.refresh(delegate);
            });
            action.setChecked(!onlineProperty.isModified());
            menu.add(action);

            action = new SimpleAction(onlineProperty.getSuggestedValue(), a -> {
                onlineProperty.setModified(true);
                tableViewer.refresh(delegate);
            });
            action.setChecked(onlineProperty.isModified());
            menu.add(action);
        }

        menuManager.add(new Separator());

        MenuManager menu = new MenuManager("Development");
        for (OnlineItem onlineItem : delegate.getOnlineItems())
            menu.add(new LabelOnly(onlineItem.getId() + ' ' + onlineItem.getName() + ' ' + onlineItem.getIsin()));
        menuManager.add(menu);
    }

    private void runOnlineSync()
    {
        try
        {
            getContainer().run(true, true, progress -> {
                progress.beginTask("Datenabgleich durchführen", securities.size());

                for (OnlineSecurity delegate : securities)
                {
                    if (progress.isCanceled())
                        break;
                    delegate.checkOnline();
                    progress.worked(1);
                }

                Display.getDefault().syncExec(() -> tableViewer.setInput(securities));
            });
        }
        catch (InvocationTargetException | InterruptedException e)
        {
            PortfolioPlugin.log(e);
        }
    }

}
