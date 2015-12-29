// A toggle switch ordered from https://www.sparkfun.com/products/11310

$fn = 16;

length = 28;
width = 16.55;
height = 19.12;

toggle_switch_with_cover();

module base() {
    cube([length, width, height]);
}

module prongs() {
    translate([1.5, width/5, -10.6]) cube([4, 3*width/5, 10.6]);
    translate([length-1.5-4, width/5, -10.6]) cube([4, 3*width/5, 10.6]);
    translate([-9, 1.5, -10.5]) cube([12, width/2 - 1.5, 26.5]);
}

module shaft() {
    translate([length/2, width/2, height]) cylinder(h = 13, r = 11.74 / 2);
    translate([length/2, width/2, height + 3]) cylinder(h = 6, r = 16.55 / 2);
}

module plate() {
    translate([0, 0, height + 6]) cube([40.6, 17, .9]);
}

module toggle() {
    translate([length/2, width/2, height + 9]) rotate([0, -15, 0]) {
        cylinder(h = 15, r = 7.22/2);    
    }
}

module cover() {
    color("red") translate([0, 0, height + 6.82])
        cube([40.6, width, 43.9 - 17.56]);
}

module toggle_switch_with_cover() {
    translate([0, 0, -height - .82 - 6]) 
        union() {
            base();
            color("silver") prongs();
            color("black") shaft();
            color("silver") plate();
            color("red") toggle();
            color("red") cover();
        }
}