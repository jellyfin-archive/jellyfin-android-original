package acidhax.cordova.chromecast;

import java.util.ArrayList;
import java.util.Collection;

import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;

public class ChromecastMediaRouterCallback extends MediaRouter.Callback {
	private volatile ArrayList<RouteInfo> routes = new ArrayList<RouteInfo>();
	
	private Chromecast callback = null;
	
	public void registerCallbacks(Chromecast instance) {
		this.callback = instance;
	}

	public synchronized RouteInfo getRoute(String id) {
		for (RouteInfo i : this.routes) {
			if (i.getId().equals(id)) {
				return i;
			}
		}
		return null;
	}

	public synchronized RouteInfo getRoute(int index) {
		return routes.get(index);
	}

	public synchronized Collection<RouteInfo> getRoutes() {
		return routes;
	}

	@Override
	public synchronized void onRouteAdded(MediaRouter router, RouteInfo route) {
		routes.add(route);
		if (this.callback != null) {
			this.callback.onRouteAdded(router, route);
		}
	}
	
	@Override
	public void onRouteRemoved(MediaRouter router, RouteInfo route) {
		routes.remove(route);
		if (this.callback != null) {
			this.callback.onRouteRemoved(router, route);
		}
	}
	
	@Override
	public void onRouteSelected(MediaRouter router, RouteInfo info) {
		if (this.callback != null) {
			this.callback.onRouteSelected(router, info);
		}
	}
	
	@Override
	public void onRouteUnselected(MediaRouter router, RouteInfo info) {
		if (this.callback != null) {
			this.callback.onRouteUnselected(router, info);
		}
	}
}
