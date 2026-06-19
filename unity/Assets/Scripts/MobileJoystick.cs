using UnityEngine;
using UnityEngine.EventSystems;

namespace Wildland3D
{
    public sealed class MobileJoystick : MonoBehaviour, IPointerDownHandler, IDragHandler, IPointerUpHandler
    {
        public RectTransform baseCircle;
        public RectTransform knob;
        public float radius = 120f;
        public float deadZone = .08f;
        public Vector2 Value { get; private set; }

        private Vector2 center;

        private void Start()
        {
            if (baseCircle) center = baseCircle.anchoredPosition;
            if (knob) knob.anchoredPosition = center;
        }

        public void OnPointerDown(PointerEventData eventData) => UpdateStick(eventData);
        public void OnDrag(PointerEventData eventData) => UpdateStick(eventData);
        public void OnPointerUp(PointerEventData eventData)
        {
            Value = Vector2.zero;
            if (knob) knob.anchoredPosition = center;
        }

        private void UpdateStick(PointerEventData eventData)
        {
            if (!baseCircle) return;
            RectTransformUtility.ScreenPointToLocalPointInRectangle((RectTransform)baseCircle.parent, eventData.position, eventData.pressEventCamera, out var local);
            Vector2 delta = local - center;
            delta = Vector2.ClampMagnitude(delta, radius);
            if (knob) knob.anchoredPosition = center + delta;
            Value = delta / radius;
            if (Value.magnitude < deadZone) Value = Vector2.zero;
        }
    }
}
