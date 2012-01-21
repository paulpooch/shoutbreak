(function($) {

	$.fn.fullSlider = function(options) {

		var defaults = {
			pause: 8000,
			duration: 1000,
			minWidth: 100
		};

		var options = $.extend(defaults, options); 

		this.each(function() {

			var slider = $(this);
			var slides = [];
			var width = 0;
			var pid = 0;
			var current = 0;
			var isPaused = true;

			$(window).resize(function() {

				resize();

			});

			var resize = function() {

				width = slider.width();

				if (width < options.minWidth) {

					width = options.minWidth;

				}

				if (!isPaused) {

					setTimeout(resize, 200);

				} else {

					$("#slider li").each(function() {

						var slide = $(this);
						slide.width(width);

					});

					$("#slider ul").css({ "width": slides.length * width, marginLeft: -1 * current * width  });
				}

			};

			var start = function() {

				pid = setTimeout(step, options.pause);

			};

			var step = function() {

				var next = current + 1;
				goto(next);
	
			};

			var goto = function(index) {

				if (isPaused) {

					if (index > slides.length - 1) {

						index = 0;

					} else if (index < 0) {

						index = slides.length - 1;

					}

					stop();
					var offset = (current - index) * width;
					isPaused = false;
					$("#slider ul").animate({ marginLeft: "+=" + offset }, options.duration, function() {

						isPaused = true;
						$("#controls ." + current).removeClass("current");
						current = index;
						$("#controls ." + current).addClass("current");
						pid = setTimeout(step, options.pause);

					});

				}

			};

			var stop = function() {

				clearInterval(pid);

			};

			var constructor = (function() {

				var id = 0;

				// set static properties
				$("#slider").css("overflow", "hidden");
				$("#slider li").css("overflow", "hidden");
				$("#slider ul").css({ margin: 0, padding: 0, "list-style-type" : "none" });
				$("#slider").append('<div id="controls">');
				$("#slider li").each(function() {

					var slide = $(this);
					slide.css("float", "left");
					slides.push(slide);

					// add navigation buttons
					$("#controls").append('<a href="#" class="' + id + '"></a>');
					id++;

				});
				$("#slider").append('</div>');

				// register control listeners
				$("#controls a").click(function() {
					var id = $(this).attr("class");
					goto(id);
				});

				// register left arrow key listener
				$(document).keydown(function(e) {

					if (e.keyCode == 37) {
						goto(current - 1);
						return false;
					}

				});

				// register right arrow key listener
				$(document).keydown(function(e) {

					if (e.keyCode == 39) { 
						goto(current + 1);
						return false;

					}

				});

				$("#controls .0").addClass("current");

				resize();
				start();

			})();
	
		});

	};

})( jQuery );
