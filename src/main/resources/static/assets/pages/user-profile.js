'use strict';
$(document).ready(function() {

    $(window).on('resize', function() {
        dashboardEcharts();
    });
    $("a[data-toggle=\"tab\"]").on("shown.bs.tab", function(e) {
        dashboardEcharts();
    });


    $("a[data-toggle=\"tab\"]").on("shown.bs.tab", function(e) {
        $($.fn.dataTable.tables(true)).DataTable().columns.adjust();
    });

    //    Edit information of user-profile
    $('#edit-cancel').on('click', function() {

        var c = $('#edit-btn').find("i");
        c.removeClass('icofont-close');
        c.addClass('icofont-edit');
        $('.view-info').show();
        $('.edit-info').hide();

    });

    $('.edit-info').hide();


    $('#edit-btn').on('click', function() {
        var b = $(this).find("i");
        var edit_class = b.attr('class');
        if (edit_class == 'icofont icofont-edit') {
            b.removeClass('icofont-edit');
            b.addClass('icofont-close');
            $('.view-info').hide();
            $('.edit-info').show();
        } else {
            b.removeClass('icofont-close');
            b.addClass('icofont-edit');
            $('.view-info').show();
            $('.edit-info').hide();
        }
    });
    //edit user description
    $('#edit-cancel-btn').on('click', function() {

        var c = $('#edit-info-btn').find("i");
        c.removeClass('icofont-close');
        c.addClass('icofont-edit');
        $('.view-desc').show();
        $('.edit-desc').hide();

    });

    $('.edit-desc').hide();


    $('#edit-info-btn').on('click', function() {
        var b = $(this).find("i");
        var edit_class = b.attr('class');
        if (edit_class == 'icofont icofont-edit') {
            b.removeClass('icofont-edit');
            b.addClass('icofont-close');
            $('.view-desc').hide();
            $('.edit-desc').show();
        } else {
            b.removeClass('icofont-close');
            b.addClass('icofont-edit');
            $('.view-desc').show();
            $('.edit-desc').hide();
        }
    });
        // Mini-color js start
        $('.demo').each(function() {
            //
            // Dear reader, it's actually very easy to initialize MiniColors. For example:
            //
            //  $(selector).minicolors();
            //
            // The way I've done it below is just for the demo, so don't get confused
            // by it. Also, data- attributes aren't supported at this time...they're
            // only used for this demo.
            //
            $(this).minicolors({
                control: $(this).attr('data-control') || 'hue',
                defaultValue: $(this).attr('data-defaultValue') || '',
                format: $(this).attr('data-format') || 'hex',
                keywords: $(this).attr('data-keywords') || '',
                inline: $(this).attr('data-inline') === 'true',
                letterCase: $(this).attr('data-letterCase') || 'lowercase',
                opacity: $(this).attr('data-opacity'),
                position: $(this).attr('data-position') || 'bottom left',
                swatches: $(this).attr('data-swatches') ? $(this).attr('data-swatches').split('|') : [],
                change: function(value, opacity) {
                    if (!value) return;
                    if (opacity) value += ', ' + opacity;
                    if (typeof console === 'object') {
                        console.log(value);
                    }
                },
                theme: 'bootstrap'
            });

        });
    // Mini-color js ends
});
