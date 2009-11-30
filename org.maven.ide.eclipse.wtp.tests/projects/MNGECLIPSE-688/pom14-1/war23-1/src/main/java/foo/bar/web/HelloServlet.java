package foo.bar.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import foo.bar.services.HelloServiceLocal;
import foo.bar.services.HelloServiceUtil;

public class HelloServlet extends HttpServlet {

	private static final long serialVersionUID = 8071579275104532537L;

	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		String message;
		String name = req.getParameter("name");
		try {
	
			HelloServiceLocal helloService = HelloServiceUtil.getLocalHome().create();
			message = helloService.sayHello(name);
		} catch (Exception e) {
			e.printStackTrace();
			message = e.getMessage();
		}
		req.setAttribute("message", message);
		getServletContext().getRequestDispatcher("/index2.jsp").forward(req,
				resp);
	}
}
