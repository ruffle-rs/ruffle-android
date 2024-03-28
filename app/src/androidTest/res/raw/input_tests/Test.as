package  {
	import flash.display.MovieClip;
	import flash.events.MouseEvent;
	import flash.display.DisplayObject;
	import flash.events.KeyboardEvent;
	
	public class Test extends MovieClip {
		public function Test() {
			trace("Test started!");
			
			addKeyListeners("stage", stage);
			addMouseListeners("red", red);
			addMouseListeners("blue", blue);
		}
		
		function addMouseListeners(name: String, clip: DisplayObject) {
			var listener = function(event: MouseEvent) {
				trace(name + " received " + event.type);
			};
			clip.addEventListener(MouseEvent.MOUSE_DOWN, listener);
			clip.addEventListener(MouseEvent.MOUSE_UP, listener);
			//clip.addEventListener(MouseEvent.MOUSE_OVER, listener);
			//clip.addEventListener(MouseEvent.MOUSE_OUT, listener);
			clip.addEventListener(MouseEvent.CLICK, listener);
		}
		
		function addKeyListeners(name: String, clip: DisplayObject) {
			var listener = function(event: KeyboardEvent) {
				trace(event.type + ": keyCode = " + event.keyCode + ", charCode = " + event.charCode);
			};
			stage.addEventListener(KeyboardEvent.KEY_DOWN, listener);
			stage.addEventListener(KeyboardEvent.KEY_UP, listener);
		}
	}
}
