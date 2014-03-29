public class Memoizer {
    public interface Function { //<expensiveFunction>
      // does not take or return nil
      Object call (Object x); //<compute>
    }

    // Return a bounded memoized version of fn 'f' 
    // that caches the last 'k' computed values
    public static Function boundedMemoize(final Function f, final int k) { 
    	class boundedFunction implements Function{
    		
    		//Need a hashmap for constant time access to function return values and a queue to track
    		//which object to remove in case max size is reached.
    		private ConcurrentHashMap<Object, Future<Object>> cache = new ConcurrentHashMap<Object, Future<Object>>(); 
    		private LinkedBlockingQueue<Object> functionQueue = new LinkedBlockingQueue<Object>(k);
    		
    		//Store the function, number of elements and max num of elements.
    		private Function func;
        	private int maxElements;
        	volatile int numElements = 0;
        	
        	public boundedFunction(Function f, int k){
        		this.func = f;
        		this.maxElements = k;
        	}
        	
			@Override
			public Object call(final Object x) {
				Future<Object> result = cache.get(x);
				if(result == null){
						Callable<Object> eval = new Callable<Object>() {
							public Object call() throws InterruptedException {
								return f.call(x);
						 	}
						}; 
					//Use FutureTask for blocking until computation is completed:
					FutureTask<Object> ft = new FutureTask<Object>(eval);
					result = ft;
					//Calls to queue, cache, and numElements must be executed atomically:
					synchronized(this){
						if(numElements >= maxElements){
							Object oldHead = functionQueue.poll();
							cache.remove(oldHead);
							numElements--;
						}
						try {
							functionQueue.put(x);
							cache.put(x, ft);
							numElements++;
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				//Where the magic happens.  Outside of locks to allow for concurrency.	
				ft.run();
				}
				try{
					return result.get();
				}
				catch (Exception e) {
					System.out.println("Future 'get' failure");
				}
				return null; 
			}	
    	}
    	return new boundedFunction(f, k);
    }
}