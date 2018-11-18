package edu.dbgroup.logic.models;

/**
 * Singleton class which contains instances of the GUI's local model (not to be confused with the databases model).
 *
 * This GUI can use these models to "bind" values. In other words, when a bound value changes in the Model, the GUI
 * automatically updates to reflect this change without any additional calls.
 */
public enum ServiceProvider {

    INSTANCE;

    private final HomeViewModel homeViewModel = new HomeViewModel();
    private final KansasMapModel kansasMapModel = new KansasMapModel();

    public HomeViewModel getHomeViewModel() {
        return homeViewModel;
    }

    public KansasMapModel getKansasMapModel() {
        return kansasMapModel;
    }
}
