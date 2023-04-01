//
//  VCSettingViewController.swift
//  VC
//
//  Created by Nathan A on 2/1/23.
//

import UIKit
import SwiftUI
import MobileCoreServices

/// Controller to view and change the application's settings
final class VCSettingViewController: UIViewController {
    var countdownTimer: Timer?
    
    override func viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = .systemBackground
        title = "Settings"

        let button = UIButton(type: .system)
        button.frame = CGRect(x: 0, y: 0, width: 100, height: 50)
        button.center = view.center
        button.setTitle("Test MSG", for: .normal)
        button.addTarget(self, action: #selector(sendMessageButtonTapped), for: .touchUpInside)
        view.addSubview(button)

        let button2 = UIButton(type: .system)
        button2.frame = CGRect(x: 0, y: button.frame.maxY + 20, width: 100, height: 50)
        button2.center.x = view.center.x
        button2.setTitle("Make Call", for: .normal)
        button2.addTarget(self, action: #selector(callButtonTapped), for: .touchUpInside)
        view.addSubview(button2)

        let button3 = UIButton(type: .system)
        button3.frame = CGRect(x: 0, y: button2.frame.maxY + 20, width: 100, height: 50)
        button3.center.x = view.center.x
        button3.setTitle("Countdown", for: .normal)
        button3.addTarget(self, action: #selector(countDownButtonTapped), for: .touchUpInside)
        view.addSubview(button3)
    }

    @objc func sendMessageButtonTapped() {
        let vcContacts = VCContactsViewController()
        vcContacts.textMessageWithTwilio()
    }
    
    @objc func callButtonTapped() {
        let vcContacts = VCContactsViewController()
        vcContacts.callWithTwilio()
    }
    
    @objc func countDownButtonTapped() {
        let countDownTitle = "Crash Detected!"
        let countDownMessage = "\nTo cancel automatic notifications, press 'Cancel'"
        let alertController = UIAlertController(title: countDownTitle, message: countDownMessage, preferredStyle: .alert)

        // Change countDownTitle attributes
        if let titleString = countDownTitle as? NSString {
            let attributes: [NSAttributedString.Key: Any] = [
                .foregroundColor: UIColor(named: "CountDownColor") as Any,
                .font: UIFont.boldSystemFont(ofSize: 30)
            ]
            let attributedTitle = NSAttributedString(string: titleString as String, attributes: attributes)
            alertController.setValue(attributedTitle, forKey: "attributedTitle")
        }
        
        // Change countDownMessage color and font size
        if let messageString = countDownMessage as? NSString {
            let attributes = [
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 15)
            ]
            let attributedMessage = NSAttributedString(string: messageString as String, attributes: attributes)
            alertController.setValue(attributedMessage, forKey: "attributedMessage")
        }
        
        // Add text label
        let countDownTextLabel = UILabel()
        countDownTextLabel.font = UIFont.systemFont(ofSize: 16)
        countDownTextLabel.textAlignment = .center
        countDownTextLabel.text = "Time Until Emergency Alerts"
        alertController.view.addSubview(countDownTextLabel)
        countDownTextLabel.translatesAutoresizingMaskIntoConstraints = false
        countDownTextLabel.centerXAnchor.constraint(equalTo: alertController.view.centerXAnchor).isActive = true
        countDownTextLabel.bottomAnchor.constraint(equalTo: alertController.view.centerYAnchor, constant: 20).isActive = true
        
        // Create the countdown label and add it to the alert controller
        let countDownLabel = UILabel(frame: CGRect(x: 0, y: 0, width: 100, height: 100))
        countDownLabel.font = UIFont.systemFont(ofSize: 40)
        countDownLabel.textColor = UIColor(named: "CountDownColor")
        countDownLabel.textAlignment = .center
        countDownLabel.text = "10"
        alertController.view.addSubview(countDownLabel)
        countDownLabel.translatesAutoresizingMaskIntoConstraints = false
        countDownLabel.centerXAnchor.constraint(equalTo: alertController.view.centerXAnchor).isActive = true
        countDownLabel.centerYAnchor.constraint(equalTo: alertController.view.centerYAnchor, constant: 60).isActive = true

        // Create the cancel action and add it to the alert controller
        let cancelAction = UIAlertAction(title: "Cancel", style: .cancel) { [weak self] _ in
            self?.dismiss(animated: true, completion: nil)
        }
        alertController.addAction(cancelAction)

        // Start the countdown timer
        var countdownSeconds = 10
        let countdownTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { timer in
            countdownSeconds -= 1
            countDownLabel.text = "\(countdownSeconds)"
            if countdownSeconds == 0 {
                timer.invalidate()
                self.dismiss(animated: true, completion: nil)
            }
        }

        // Store the countdown timer in a property so it can be invalidated if necessary
        self.countdownTimer = countdownTimer

        // Set the alert controller's height and width
        let height: NSLayoutConstraint = NSLayoutConstraint(item: alertController.view!, attribute: NSLayoutConstraint.Attribute.height, relatedBy: NSLayoutConstraint.Relation.equal, toItem: nil, attribute: NSLayoutConstraint.Attribute.notAnAttribute, multiplier: 1, constant: 350)
        let width:NSLayoutConstraint = NSLayoutConstraint(item: alertController.view!, attribute: NSLayoutConstraint.Attribute.width, relatedBy: NSLayoutConstraint.Relation.equal, toItem: nil, attribute: NSLayoutConstraint.Attribute.notAnAttribute, multiplier: 1, constant: 500)
        alertController.view.addConstraint(height)
        alertController.view.addConstraint(width)

        // Present the alert controller
        present(alertController, animated: true, completion: nil)
    }
}
