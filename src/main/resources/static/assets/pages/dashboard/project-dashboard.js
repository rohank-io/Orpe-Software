$(document).ready(function () {

    // Extra chart
    Morris.Area({
        element: 'morris-extra-area',
        data: [{
            period: '2010',
            iphone: 0,
            ipad: 0,
            itouch: 0
        }, {
            period: '2011',
            iphone: 100,
            ipad: 15,
            itouch: 65
        }, {
            period: '2012',
            iphone: 20,
            ipad: 135,
            itouch: 0
        }, {
            period: '2013',
            iphone: 100,
            ipad: 12,
            itouch: 25
        }, {
            period: '2014',
            iphone: 50,
            ipad: 20,
            itouch: 100
        }, {
            period: '2015',
            iphone: 25,
            ipad: 100,
            itouch: 40
        }, {
            period: '2016',
            iphone: 10,
            ipad: 10,
            itouch: 10
        }


        ],
        lineColors: ['#fb9678', '#7E81CB', '#01C0C8'],
        xkey: 'period',
        ykeys: ['iphone', 'ipad', 'itouch'],
        labels: ['Site A', 'Site B', 'Site C'],
        pointSize: 0,
        lineWidth: 0,
        resize: true,
        fillOpacity: 0.8,
        behaveLikeLine: true,
        gridLineColor: '#5FBEAA',
        hideHover: 'auto'

    });

    // Morris bar chart

    Morris.Bar({
        element: 'analythics-graph',
        barSizeRatio:0.2,
        data: [  {
            y: '2009',
            a: 75
        }, {
            y: '2010',
            a: 50
        }, {
            y: '2011',
            a: 42
        }, {
            y: '2012',
            a: 32
        }, {
            y: '2013',
            a: 56
        }, {
            y: '2014',
            a: 60
        }, {
            y: '2015',
            a: 47
        }, {
            y: '2016',
            a: 48
        }
        ],
        xkey: 'y',
        lineWidth:'10px',
        ykeys: ['a'],
        labels: ['A'],
        barColors: ['#0073AA'],
        hideHover: 'auto',
        gridLineColor: '#ddd',
        resize: true
    });
});
