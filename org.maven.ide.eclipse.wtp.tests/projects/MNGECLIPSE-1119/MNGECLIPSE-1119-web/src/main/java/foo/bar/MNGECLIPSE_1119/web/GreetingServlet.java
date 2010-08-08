package foo.bar.MNGECLIPSE_1119.web;

import java.io.IOException;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import foo.bar.MNGECLIPSE_1119.ejb.HelloService;

/**
 * Greeting servlet
 */
public class GreetingServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@EJB
	private HelloService helloService;
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String name = request.getParameter("name");
		String greeting = helloService.greet(name);
		request.setAttribute("greeting", greeting);
		getServletContext().getRequestDispatcher("/index.jsp").forward(request, response); //$NON-NLS-1$
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

}
