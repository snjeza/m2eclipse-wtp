package foo.bar.main;

import foo.bar.dep.Dep;

public class Main {
	
	public void stuff() {
		final Dep dep = new Dep();
		dep.noOp();
	}
}
