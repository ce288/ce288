/**
 * 
 */
package ce288.client;


import java.io.InputStream;
import java.util.UUID;

import ce288.tasks.Task;
import ce288.tasks.TaskRepositoryInterface;


public class FileEmbrace extends AbstractFileAnalyser {


	public FileEmbrace() {
		// TODO Auto-generated constructor stub
	}


	@Override
	public void process(UUID clientId, InputStream in, Task task,
			TaskRepositoryInterface stub) {
		// TODO Auto-generated method stub
		

		String line;
		line = null;
		
		
        while(line != null){
        	
          line = in.toString();
          System.out.println("Varlei");
          System.out.println(line);
        
        }   
	} // close process


	public void teste(String message){
		
		int contador = 0;
		while(contador < 40)
		{ System.out.println(message);
		  contador++;
		}
		
		return;
		
	} // close teste 
	
	
	

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	} // close main()

} // close FileEmbrace
