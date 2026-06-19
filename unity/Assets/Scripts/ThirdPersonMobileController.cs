using UnityEngine;

namespace Wildland3D
{
    public sealed class ThirdPersonMobileController : MonoBehaviour
    {
        public Transform cameraPivot;
        public Camera gameCamera;
        public float moveSpeed = 6.5f;
        public float turnLerp = 14f;
        public float cameraDistance = 9.5f;
        public float cameraHeight = 3.2f;
        public MobileJoystick joystick;
        public ProceduralHeroAnimator heroAnimator;

        private Vector3 velocity;
        private float yaw = 35f;
        private float pitch = 22f;

        private void Awake()
        {
            if (!gameCamera) gameCamera = Camera.main;
            if (!cameraPivot) cameraPivot = transform;
            Application.targetFrameRate = 90;
            QualitySettings.vSyncCount = 0;
        }

        private void Update()
        {
            Vector2 input = joystick ? joystick.Value : new Vector2(Input.GetAxisRaw("Horizontal"), Input.GetAxisRaw("Vertical"));
            if (Input.touchCount > 0)
            {
                foreach (var t in Input.touches)
                {
                    if (t.position.x > Screen.width * .5f && t.phase == TouchPhase.Moved)
                    {
                        yaw += t.deltaPosition.x * .12f;
                        pitch = Mathf.Clamp(pitch - t.deltaPosition.y * .08f, 10f, 48f);
                    }
                }
            }
            Vector3 forward = Quaternion.Euler(0, yaw, 0) * Vector3.forward;
            Vector3 right = Quaternion.Euler(0, yaw, 0) * Vector3.right;
            Vector3 move = (right * input.x + forward * input.y);
            float speed = Mathf.Clamp01(input.magnitude);
            if (move.sqrMagnitude > .001f)
            {
                move.Normalize();
                velocity = move * moveSpeed;
                transform.rotation = Quaternion.Slerp(transform.rotation, Quaternion.LookRotation(move), turnLerp * Time.deltaTime);
            }
            else velocity = Vector3.Lerp(velocity, Vector3.zero, 1f - Mathf.Pow(.025f, Time.deltaTime));
            transform.position += velocity * Time.deltaTime;
            if (heroAnimator) heroAnimator.SetMoveAmount(speed);
            UpdateCamera();
        }

        private void UpdateCamera()
        {
            if (!gameCamera) return;
            Quaternion rot = Quaternion.Euler(pitch, yaw, 0);
            Vector3 desired = transform.position + Vector3.up * cameraHeight - rot * Vector3.forward * cameraDistance;
            gameCamera.transform.position = Vector3.Lerp(gameCamera.transform.position, desired, 1f - Mathf.Pow(.003f, Time.deltaTime));
            gameCamera.transform.LookAt(transform.position + Vector3.up * 1.6f);
        }
    }
}
