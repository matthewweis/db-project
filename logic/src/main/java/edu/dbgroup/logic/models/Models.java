package edu.dbgroup.logic.models;

public final class Models {

    private final HomeViewModel homeViewModel = new HomeViewModel();
    private final KansasMapModel kansasMapModel = new KansasMapModel();

    public HomeViewModel getHomeViewModel() {
        return homeViewModel;
    }

    public KansasMapModel getKansasMapModel() {
        return kansasMapModel;
    }
}
