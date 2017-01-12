var mame = $('.mainMenu');

function doMenu(obj) {
    var hashTag = obj.attr('id') + '_child';
    obj.on('click', function () {
        $('#' + hashTag).toggle();
        return false;
    });
}
function doLoad(obj) {
    obj.on('click', function () {
        $('.mainMenu .active').removeClass('active');
        obj.addClass('active');
    });
}

$(document).ready(function () {
    $.each($('.mParent'), function (i, val) {
        doMenu($(val));
    });
    $.each($('li[role="presentation"][class!="mParent"]'), function (i, val) {
        doLoad($(val));
    });
    mame.width($('.menuCont').width());
    mame.height($(window).height() - mame[0].getBoundingClientRect().top);

    $("a[href^='#']").click(function(event) {

    });

    $.each($("a[href^='#']"),function(i,val) {
        var hr = $(val).attr('href');
        var id = hr.substring(1);
        if (id !== "") {
			$(val).click(function(event) {
				$('html, body').animate({
					scrollTop: $(document.getElementById($(val).attr("href").substring(1))).offset().top
				}, 500);
			})
        }
    })
});
$(window).resize(function () {
    mame.width($('.menuCont').width());
    $('.menuCont').height(mame.height());
    mame.height($(window).height() - mame[0].getBoundingClientRect().top);
});

function DoubleScroll(element) {
    var scrollbar = document.createElement('div');
    scrollbar.appendChild(document.createElement('div'));
    scrollbar.style.overflow = 'auto';
    scrollbar.style.overflowY = 'hidden';
    scrollbar.className += " dobscrollDiv";
    scrollbar.firstChild.style.width = element.scrollWidth + 'px';
    //scrollbar.firstChild.style.paddingTop= '1px';
    scrollbar.firstChild.appendChild(document.createTextNode('\xA0'));
    scrollbar.onscroll = function () {
        element.scrollLeft = scrollbar.scrollLeft;
    };
    element.onscroll = function () {
        scrollbar.scrollLeft = element.scrollLeft;
    };
    element.parentNode.insertBefore(scrollbar, element);
}

DoubleScroll(document.getElementById('doublescroll'));
$('#co-1Table').height($('.dobscrollDiv').height());