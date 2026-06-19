extends Control

var stick_center := Vector2.ZERO
var stick_knob := Vector2.ZERO
var hud_scale := 1.0
var fps := 0
var title := "WILDLAND"

func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	set_anchors_preset(Control.PRESET_FULL_RECT)

func _draw() -> void:
	var r := 104.0 * hud_scale
	var kr := 42.0 * hud_scale
	draw_circle(stick_center, r + 12.0 * hud_scale, Color(0.015,0.017,0.016,0.50))
	draw_circle(stick_center, r, Color(0.10,0.12,0.11,0.58))
	draw_arc(stick_center, r, 0, TAU, 96, Color(0.72,0.70,0.62,0.30), 2.0 * hud_scale)
	draw_circle(stick_knob, kr, Color(0.42,0.45,0.40,0.86))
	draw_circle(stick_knob + Vector2(-kr*0.22, -kr*0.22), kr*0.30, Color(0.95,0.92,0.80,0.18))
	var panel := Rect2(Vector2(size.x - 242.0 * hud_scale, 22.0 * hud_scale), Vector2(220.0 * hud_scale, 54.0 * hud_scale))
	draw_rect(panel, Color(0.01,0.012,0.012,0.38), true)
	draw_rect(panel, Color(0.70,0.68,0.58,0.18), false, 1.5 * hud_scale)
	var font := get_theme_default_font()
	var font_size := int(21 * hud_scale)
	draw_string(font, Vector2(20 * hud_scale, 35 * hud_scale), title, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size, Color(0.86,0.86,0.78,1))
	draw_string(font, panel.position + Vector2(16 * hud_scale, 35 * hud_scale), "FPS " + str(fps), HORIZONTAL_ALIGNMENT_LEFT, -1, font_size, Color(0.86,0.86,0.78,1))
	draw_string(font, stick_center + Vector2(-28*hud_scale, r + 25*hud_scale), "MOVE", HORIZONTAL_ALIGNMENT_LEFT, -1, int(16*hud_scale), Color(0.78,0.78,0.70,0.82))
