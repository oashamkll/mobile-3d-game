extends Node3D

# Wildland 3D Adventure — Godot 4 mobile engine implementation.
# Realistic, darker art direction; infinite chunk world; mobile HUD joystick;
# pooled instances for FPS; procedural high-detail humanoid with animation.

const CHUNK_SIZE := 56.0
const CHUNK_RADIUS := 2
const HERO_SPEED := 7.0
const JOYSTICK_RADIUS := 128.0

var rng := RandomNumberGenerator.new()
var camera: Camera3D
var sun: DirectionalLight3D
var world_env: WorldEnvironment
var hero_root: Node3D
var hero_parts: Dictionary = {}
var chunks: Array[Dictionary] = []
var chunk_center := Vector2i(999999, 999999)
var pending_chunk_coords: Array[Vector2i] = []
var pending_chunk_index := 0

var hero_pos := Vector3.ZERO
var velocity := Vector3.ZERO
var camera_yaw := 0.55
var camera_pitch := 0.38
var walk_time := 0.0
var last_step := -1

var move_vec := Vector2.ZERO
var stick_pointer := -1
var look_pointer := -1
var last_look := Vector2.ZERO
var fixed_stick_center := Vector2.ZERO
var stick_knob := Vector2.ZERO
var hud_scale := 1.0

var mat_ground: StandardMaterial3D
var mat_stone: StandardMaterial3D
var mat_bark: StandardMaterial3D
var mat_leaves: StandardMaterial3D
var mat_cloth: StandardMaterial3D
var mat_skin: StandardMaterial3D
var mat_leather: StandardMaterial3D
var mat_hair: StandardMaterial3D
var mat_metal: StandardMaterial3D

var footstep: AudioStreamPlayer
var wind: AudioStreamPlayer
var chime: AudioStreamPlayer
var hud: Control

func _ready() -> void:
	Engine.max_fps = 90
	RenderingServer.viewport_set_msaa_3d(get_viewport().get_viewport_rid(), RenderingServer.VIEWPORT_MSAA_DISABLED)
	_build_materials()
	_build_lighting()
	_build_camera()
	_build_audio()
	_build_hud()
	_build_hero()
	_build_chunks()
	_relayout_hud()
	Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED if OS.has_feature("editor") == false else Input.MOUSE_MODE_VISIBLE)

func _build_materials() -> void:
	mat_ground = _pbr("res://assets/textures/environment/grass_albedo.png", Color(0.27,0.30,0.24), 0.96, 0.0)
	mat_stone = _pbr("res://assets/textures/environment/stone_albedo.png", Color(0.42,0.41,0.38), 0.90, 0.0)
	mat_bark = _pbr("res://assets/textures/environment/wood_albedo.png", Color(0.30,0.22,0.17), 0.88, 0.0)
	mat_leaves = _pbr("res://assets/textures/environment/leaves_albedo.png", Color(0.20,0.25,0.18), 0.92, 0.0)
	mat_cloth = _pbr("res://assets/textures/character/hero_cloth_albedo.png", Color(0.12,0.16,0.20), 0.78, 0.0)
	mat_skin = _pbr("res://assets/textures/character/hero_skin_albedo.png", Color(0.62,0.45,0.34), 0.61, 0.0)
	mat_leather = _pbr("res://assets/textures/character/hero_leather_albedo.png", Color(0.30,0.21,0.16), 0.68, 0.05)
	mat_hair = _pbr("res://assets/textures/character/hero_hair_albedo.png", Color(0.12,0.09,0.07), 0.76, 0.0)
	mat_metal = _pbr("res://assets/textures/character/hero_metal_albedo.png", Color(0.70,0.73,0.74), 0.34, 0.45)

func _pbr(path: String, tint: Color, roughness: float, metallic: float) -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.albedo_color = tint
	m.roughness = roughness
	m.metallic = metallic
	m.texture_filter = BaseMaterial3D.TEXTURE_FILTER_LINEAR_WITH_MIPMAPS
	if ResourceLoader.exists(path):
		m.albedo_texture = load(path)
	return m

func _build_lighting() -> void:
	world_env = WorldEnvironment.new()
	var env := Environment.new()
	env.background_mode = Environment.BG_COLOR
	env.background_color = Color(0.46, 0.54, 0.58)
	env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	env.ambient_light_color = Color(0.55, 0.57, 0.55)
	env.ambient_light_energy = 0.78
	env.fog_enabled = false
	env.fog_light_color = Color(0.42, 0.45, 0.43)
	env.fog_density = 0.0
	env.tonemap_mode = Environment.TONE_MAPPER_FILMIC
	env.adjustment_enabled = true
	env.adjustment_brightness = 1.02
	env.adjustment_contrast = 1.08
	env.adjustment_saturation = 0.82
	world_env.environment = env
	add_child(world_env)

	sun = DirectionalLight3D.new()
	sun.name = "Low Warm Sun"
	sun.light_energy = 2.25
	sun.light_color = Color(0.95, 0.84, 0.68)
	sun.rotation_degrees = Vector3(-52, -38, 0)
	sun.shadow_enabled = true
	sun.directional_shadow_mode = DirectionalLight3D.SHADOW_ORTHOGONAL
	add_child(sun)

func _build_camera() -> void:
	camera = Camera3D.new()
	camera.fov = 64
	camera.near = 0.08
	camera.far = 190
	add_child(camera)

func _build_audio() -> void:
	wind = AudioStreamPlayer.new(); wind.stream = load("res://assets/audio/wind_loop.wav"); wind.volume_db = -20; add_child(wind); wind.play()
	chime = AudioStreamPlayer.new(); chime.stream = load("res://assets/audio/chime.wav"); chime.volume_db = -12; add_child(chime); chime.play()
	footstep = AudioStreamPlayer.new(); footstep.stream = load("res://assets/audio/footstep.wav"); footstep.volume_db = -13; add_child(footstep)

func _build_hud() -> void:
	var layer := CanvasLayer.new()
	layer.layer = 20
	add_child(layer)
	hud = preload("res://scripts/Hud.gd").new()
	layer.add_child(hud)

func _build_hero() -> void:
	hero_root = Node3D.new()
	hero_root.name = "Procedural Realistic Humanoid"
	add_child(hero_root)
	_add_part("torso", _capsule(0.36, 1.05, mat_cloth), Vector3(0,1.60,0), 0)
	_add_part("chest", _sphere(Vector3(0.86,0.62,0.38), mat_cloth, 16, 10), Vector3(0,1.95,0.02), 0)
	_add_part("pelvis", _box(Vector3(0.68,0.34,0.38), mat_leather), Vector3(0,1.00,0), 0)
	_add_part("neck", _cylinder(0.15,0.22,mat_skin,12), Vector3(0,2.42,0), 0)
	_add_part("head", _sphere(Vector3(0.50,0.60,0.48), mat_skin, 18, 12), Vector3(0,2.78,0.02), 0)
	_add_part("hair", _sphere(Vector3(0.54,0.24,0.50), mat_hair, 16, 8), Vector3(0,3.08,-0.03), 0)
	_add_part("eye_l", _sphere(Vector3(0.050,0.036,0.024), mat_metal, 8, 6), Vector3(-0.13,2.83,0.255), 0)
	_add_part("eye_r", _sphere(Vector3(0.050,0.036,0.024), mat_metal, 8, 6), Vector3(0.13,2.83,0.255), 0)
	_add_part("belt", _box(Vector3(0.84,0.09,0.41), mat_leather), Vector3(0,1.22,0.02), 0)
	_add_part("pack", _box(Vector3(0.58,0.78,0.24), mat_leather), Vector3(0,1.72,-0.33), 0)
	for side in [-1, 1]:
		var tag := "l" if side < 0 else "r"
		_add_part("shoulder_"+tag, _sphere(Vector3(0.24,0.24,0.24), mat_cloth, 10, 8), Vector3(0.55*side,2.04,0), side)
		_add_part("upper_arm_"+tag, _capsule(0.105,0.58, mat_cloth), Vector3(0.67*side,1.58,0), side)
		_add_part("forearm_"+tag, _capsule(0.095,0.54, mat_skin), Vector3(0.68*side,1.10,0), side)
		_add_part("hand_"+tag, _sphere(Vector3(0.18,0.18,0.18), mat_skin, 8, 6), Vector3(0.68*side,0.77,0), side)
		_add_part("thigh_"+tag, _capsule(0.145,0.66, mat_leather), Vector3(0.20*side,0.63,0), side)
		_add_part("shin_"+tag, _capsule(0.125,0.58, mat_leather), Vector3(0.21*side,0.14,0), side)
		_add_part("boot_"+tag, _box(Vector3(0.32,0.17,0.52), mat_leather), Vector3(0.21*side,-0.22,0.10), side)

func _add_part(name: String, mesh: MeshInstance3D, pos: Vector3, side: int) -> void:
	mesh.name = name
	mesh.position = pos
	hero_root.add_child(mesh)
	hero_parts[name] = {"node":mesh, "base":pos, "side":side}

func _box(size: Vector3, material: Material) -> MeshInstance3D:
	var m := BoxMesh.new(); m.size = size
	var n := MeshInstance3D.new(); n.mesh = m; n.set_surface_override_material(0, material); return n
func _sphere(scale: Vector3, material: Material, radial: int, rings: int) -> MeshInstance3D:
	var m := SphereMesh.new(); m.radial_segments = radial; m.rings = rings; m.radius = 0.5; m.height = 1.0
	var n := MeshInstance3D.new(); n.mesh = m; n.scale = scale; n.set_surface_override_material(0, material); return n
func _cylinder(radius: float, height: float, material: Material, sides: int) -> MeshInstance3D:
	var m := CylinderMesh.new(); m.top_radius = radius; m.bottom_radius = radius; m.height = height; m.radial_segments = sides
	var n := MeshInstance3D.new(); n.mesh = m; n.set_surface_override_material(0, material); return n
func _capsule(radius: float, height: float, material: Material) -> MeshInstance3D:
	var m := CapsuleMesh.new(); m.radius = radius; m.height = height; m.radial_segments = 12; m.rings = 6
	var n := MeshInstance3D.new(); n.mesh = m; n.set_surface_override_material(0, material); return n

func _build_chunks() -> void:
	for i in range((CHUNK_RADIUS * 2 + 1) * (CHUNK_RADIUS * 2 + 1)):
		chunks.append(_create_chunk())
	_refresh_chunks(true)

func _create_chunk() -> Dictionary:
	var root := Node3D.new(); add_child(root)
	var chunk := {"root":root, "ground":null, "path":null, "trees":[], "rocks":[], "grass":[]}
	chunk["ground"] = _box(Vector3(CHUNK_SIZE+0.2,0.08,CHUNK_SIZE+0.2), mat_ground); root.add_child(chunk["ground"])
	chunk["path"] = _box(Vector3(5.0,0.07,3.0), mat_stone); root.add_child(chunk["path"])
	for i in range(2):
		var tr := Node3D.new(); root.add_child(tr)
		var trunk := _cylinder(0.28, 3.6, mat_bark, 10); trunk.position.y = 1.8; tr.add_child(trunk)
		var crown := _sphere(Vector3(2.0,1.6,2.0), mat_leaves, 10, 6); crown.position.y = 4.0; tr.add_child(crown)
		var crown2 := _sphere(Vector3(1.35,1.05,1.35), mat_leaves, 8, 5); crown2.position = Vector3(0.45,4.75,-0.25); tr.add_child(crown2)
		chunk["trees"].append(tr)
	for i in range(3):
		var r := _sphere(Vector3(1.0,0.50,0.85), mat_stone, 8, 5); root.add_child(r); chunk["rocks"].append(r)
	for i in range(5):
		var g := _cylinder(0.055, 0.65, mat_ground, 5); root.add_child(g); chunk["grass"].append(g)
	return chunk

func _refresh_chunks(force: bool=false) -> void:
	var cx := floori(hero_pos.x / CHUNK_SIZE)
	var cz := floori(hero_pos.z / CHUNK_SIZE)
	if not force and chunk_center == Vector2i(cx, cz): return
	chunk_center = Vector2i(cx, cz)
	pending_chunk_coords.clear()
	for dz in range(-CHUNK_RADIUS, CHUNK_RADIUS+1):
		for dx in range(-CHUNK_RADIUS, CHUNK_RADIUS+1):
			pending_chunk_coords.append(Vector2i(cx + dx, cz + dz))
	pending_chunk_coords.sort_custom(func(a: Vector2i, b: Vector2i) -> bool:
		return Vector2(a.x - cx, a.y - cz).length_squared() < Vector2(b.x - cx, b.y - cz).length_squared()
	)
	pending_chunk_index = 0
	_process_chunk_queue(999 if force else 6)

func _process_chunk_queue(budget: int) -> void:
	while pending_chunk_index < pending_chunk_coords.size() and budget > 0:
		var coord := pending_chunk_coords[pending_chunk_index]
		_position_chunk(chunks[pending_chunk_index], coord.x, coord.y)
		pending_chunk_index += 1
		budget -= 1

func _position_chunk(c: Dictionary, cx: int, cz: int) -> void:
	var base := Vector3(cx * CHUNK_SIZE, 0, cz * CHUNK_SIZE)
	c["root"].position = base
	c["ground"].position = Vector3(0,-0.05,0)
	c["path"].position = Vector3(_signed(cx,cz,1)*4.4,0.025,_signed(cx,cz,2)*4.4)
	c["path"].rotation.y = _rand(cx,cz,3) * TAU
	for i in range(c["trees"].size()):
		var tr: Node3D = c["trees"][i]
		tr.position = Vector3(_signed(cx,cz,10+i*4)*13.2,0,_signed(cx,cz,11+i*4)*13.2)
		tr.rotation.y = _rand(cx,cz,12+i*4)*TAU
		var s := 0.72 + _rand(cx,cz,13+i*4)*0.55
		tr.scale = Vector3.ONE * s
	for i in range(c["rocks"].size()):
		var r: MeshInstance3D = c["rocks"][i]
		r.position = Vector3(_signed(cx,cz,40+i*3)*14.4,0.28,_signed(cx,cz,41+i*3)*14.4)
		r.rotation.y = _rand(cx,cz,42+i*3)*TAU
		var s := 0.5 + _rand(cx,cz,43+i*3)*1.2
		r.scale = Vector3(s, 0.55 + _rand(cx,cz,44+i*3)*0.7, s*0.85)
	for i in range(c["grass"].size()):
		var g: MeshInstance3D = c["grass"][i]
		g.position = Vector3(_signed(cx,cz,70+i*2)*15.0,0.30,_signed(cx,cz,71+i*2)*15.0)
		g.rotation = Vector3(_signed(cx,cz,72+i)*0.25,_rand(cx,cz,73+i)*TAU,_signed(cx,cz,74+i)*0.25)
		g.scale = Vector3.ONE * (0.75 + _rand(cx,cz,75+i)*0.8)

func _rand(x:int, z:int, salt:int) -> float:
	var h := int(x * 73428767) ^ int(z * 912931) ^ int(salt * 19349663)
	h = h ^ (h >> 13); h = h * 1274126177; h = h ^ (h >> 16)
	return float(h & 0x7fffffff) / float(0x7fffffff)
func _signed(x:int, z:int, salt:int) -> float: return _rand(x,z,salt) * 2.0 - 1.0

func _input(event: InputEvent) -> void:
	if event is InputEventScreenTouch:
		if event.pressed:
			if event.position.x < get_viewport().size.x * 0.48 and stick_pointer == -1:
				stick_pointer = event.index; _update_joystick(event.position)
			else:
				look_pointer = event.index; last_look = event.position
		else:
			if event.index == stick_pointer:
				stick_pointer = -1; move_vec = Vector2.ZERO; stick_knob = fixed_stick_center
			elif event.index == look_pointer:
				look_pointer = -1
	elif event is InputEventScreenDrag:
		if event.index == stick_pointer:
			_update_joystick(event.position)
		elif event.index == look_pointer:
			camera_yaw -= event.relative.x * 0.006
			camera_pitch = clamp(camera_pitch + event.relative.y * 0.004, 0.18, 0.82)
	elif event is InputEventMouseMotion and Input.mouse_mode == Input.MOUSE_MODE_CAPTURED:
		camera_yaw -= event.relative.x * 0.003
		camera_pitch = clamp(camera_pitch + event.relative.y * 0.002, 0.18, 0.82)

func _update_joystick(pos: Vector2) -> void:
	var d := pos - fixed_stick_center
	if d.length() > JOYSTICK_RADIUS * hud_scale: d = d.normalized() * JOYSTICK_RADIUS * hud_scale
	stick_knob = fixed_stick_center + d
	move_vec = Vector2(-d.x, -d.y) / (JOYSTICK_RADIUS * hud_scale)
	if move_vec.length() < 0.08: move_vec = Vector2.ZERO

func _physics_process(delta: float) -> void:
	_refresh_chunks(false)
	_process_chunk_queue(2)
	var key_move := Vector2(Input.get_action_strength("move_right")-Input.get_action_strength("move_left"), Input.get_action_strength("move_forward")-Input.get_action_strength("move_back"))
	var input_move := move_vec if move_vec.length() > key_move.length() else key_move
	var forward := Vector3(sin(camera_yaw),0,cos(camera_yaw)).normalized()
	var right := Vector3(forward.z,0,-forward.x).normalized()
	var world_move := (right * input_move.x + forward * input_move.y)
	var speed: float = min(input_move.length(), 1.0)
	if world_move.length_squared() > 0.001:
		world_move = world_move.normalized()
		velocity = world_move * HERO_SPEED
		hero_root.rotation.y = atan2(world_move.x, world_move.z)
		walk_time += delta * (8.0 + speed * 5.0)
		var step := int(walk_time / PI)
		if step != last_step:
			last_step = step
			if footstep: footstep.play()
	else:
		velocity = velocity.lerp(Vector3.ZERO, 1.0 - pow(0.025, delta))
		walk_time += delta * 1.2
	hero_pos += velocity * delta
	hero_root.position = hero_pos
	_animate_hero(speed)
	_update_camera(delta)

func _animate_hero(speed: float) -> void:
	var swing := sin(walk_time) * 0.55 * speed
	var bob: float = abs(sin(walk_time)) * 0.07 * speed
	hero_root.position.y = 0.2 + bob
	for key in hero_parts.keys():
		var p: Dictionary = hero_parts[key]
		var n: Node3D = p.node
		n.position = p.base
		n.rotation = Vector3.ZERO
		var side := float(p.side)
		if key.contains("arm") or key.contains("hand"):
			n.rotation.x = -swing * side
		elif key.contains("thigh") or key.contains("shin") or key.contains("boot"):
			n.rotation.x = swing * side * 0.75
		elif key.contains("shoulder"):
			n.rotation.z = side * (0.05 + 0.05 * speed)
		elif key == "head" or key == "hair" or key.begins_with("eye"):
			n.position.y += sin(walk_time * 0.33) * 0.015

func _update_camera(delta: float) -> void:
	var dist := 10.8
	var flat := cos(camera_pitch) * dist
	var desired := hero_pos + Vector3(-sin(camera_yaw)*flat, sin(camera_pitch)*dist + 2.8, -cos(camera_yaw)*flat)
	camera.position = camera.position.lerp(desired, 1.0 - pow(0.005, delta))
	camera.look_at(hero_pos + Vector3(0,1.65,0), Vector3.UP)

func _relayout_hud() -> void:
	var size := get_viewport().get_visible_rect().size
	hud_scale = clamp(min(size.x, size.y) / 720.0, 0.85, 1.35)
	fixed_stick_center = Vector2(215.0 * hud_scale, size.y - 205.0 * hud_scale)
	stick_knob = fixed_stick_center
	if hud:
		hud.stick_center = fixed_stick_center
		hud.stick_knob = stick_knob
		hud.hud_scale = hud_scale

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed and event.keycode == KEY_ESCAPE:
		Input.mouse_mode = Input.MOUSE_MODE_VISIBLE

func _draw_screen_hud() -> void:
	pass

func _exit_tree() -> void:
	if wind: wind.stop()
